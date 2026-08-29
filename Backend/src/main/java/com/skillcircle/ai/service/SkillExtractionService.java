package com.skillcircle.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillcircle.profile.entity.Skill;
import com.skillcircle.profile.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * AI-powered skill extraction from GitHub repository content.
 *
 * <p>Analyzes README text to extract structured skill tags beyond what the basic
 * language/topic GitHub API provides. Uses the configured chat model to parse
 * natural language descriptions and identify technologies, frameworks and tools.
 *
 * <p>Extraction strategy (in order of preference):
 * <ol>
 *   <li><b>LLM</b> — highest quality, canonical naming, understands context.</li>
 *   <li><b>Seeded catalog match</b> — scans for any of the ~210 skills seeded in
 *       Phase 2, so the fallback stays in sync with the platform taxonomy.</li>
 *   <li><b>Regex patterns</b> — a small hand-curated set covering common
 *       spelling variants the catalog may not list (e.g. "k8s", "postgres").</li>
 * </ol>
 * When the LLM is unavailable the catalog and regex results are unioned.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkillExtractionService {

    /** Max characters of README sent to the LLM, to bound token cost. */
    private static final int MAX_INPUT_CHARS = 4000;

    /** Upper bound on skills returned from a single document. */
    private static final int MAX_SKILLS = 20;

    /** How long the seeded skill catalog is cached in memory. */
    private static final long CATALOG_TTL_MILLIS = 10 * 60 * 1000L;

    /**
     * Catalog names too ambiguous to match by text scanning — they appear in
     * ordinary prose far too often to be reliable signals.
     */
    private static final Set<String> AMBIGUOUS_NAMES = Set.of(
            "api", "rest", "web", "server", "app", "cli", "sql", "css", "html");

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String SYSTEM_PROMPT = """
            You are a technical skill extractor. Given a GitHub repository README or description,
            extract all technologies, frameworks, tools, databases, and platforms mentioned.

            Return ONLY a JSON array of skill names, like:
            ["React", "Node.js", "PostgreSQL", "Docker", "AWS"]

            Rules:
            - Use canonical names (e.g., "React" not "ReactJS", "PostgreSQL" not "Postgres")
            - Include programming languages, frameworks, libraries, databases, cloud services, and tools
            - Do NOT include generic terms like "API", "REST", "web", "server"
            - Maximum 20 skills per repo
            - Return an empty array [] if no specific technologies are found
            """;

    /**
     * Hand-curated patterns for common technologies, including spelling variants.
     *
     * <p>Patterns are compiled with an explicit {@link Pattern#CASE_INSENSITIVE}
     * flag rather than an inline {@code (?i)} prefix, and every alternation is
     * wrapped in a non-capturing group. An inline flag combined with a top-level
     * alternation does not reliably apply to all branches, which previously made
     * patterns like {@code (?i)\bkubernetes\b|\bk8s\b} match case-sensitively and
     * silently miss "PostgreSQL" and "Kubernetes".
     */
    private static final List<TechPattern> TECH_PATTERNS = List.of(
            tech("\\breact\\b", "React"),
            tech("\\bnext\\.?js\\b", "Next.js"),
            tech("\\bvue\\.?js\\b", "Vue.js"),
            tech("\\bangular\\b", "Angular"),
            tech("\\bsvelte\\b", "Svelte"),
            tech("\\bnode\\.?js\\b", "Node.js"),
            tech("\\bexpress\\.?js?\\b", "Express.js"),
            tech("\\bspring\\s*boot\\b", "Spring Boot"),
            tech("\\bdjango\\b", "Django"),
            tech("\\bflask\\b", "Flask"),
            tech("\\bfastapi\\b", "FastAPI"),
            tech("\\btypescript\\b", "TypeScript"),
            tech("\\bpython\\b", "Python"),
            tech("\\bjava(?!script)\\b", "Java"),
            tech("(?:\\bgolang\\b|\\bgo\\s+lang\\b)", "Go"),
            tech("\\brust\\b", "Rust"),
            tech("\\bkotlin\\b", "Kotlin"),
            tech("\\bswift\\b", "Swift"),
            tech("(?:\\bpostgresql\\b|\\bpostgres\\b)", "PostgreSQL"),
            tech("\\bmongodb\\b", "MongoDB"),
            tech("\\bmysql\\b", "MySQL"),
            tech("\\bredis\\b", "Redis"),
            tech("\\bdocker\\b", "Docker"),
            tech("(?:\\bkubernetes\\b|\\bk8s\\b)", "Kubernetes"),
            tech("\\baws\\b", "AWS"),
            tech("(?:\\bgcp\\b|\\bgoogle\\s+cloud\\b)", "Google Cloud Platform"),
            tech("\\bazure\\b", "Microsoft Azure"),
            tech("\\bgraphql\\b", "GraphQL"),
            tech("\\btailwind(?:css)?\\b", "TailwindCSS"),
            tech("\\belasticsearch\\b", "Elasticsearch"),
            tech("\\bterraform\\b", "Terraform"));

    private final LLMClient llmClient;
    private final SkillRepository skillRepository;

    /** Cached snapshot of the seeded skill catalog, refreshed on TTL expiry. */
    private volatile List<TechPattern> catalogCache;
    private volatile long catalogLoadedAt;

    /**
     * Extract skills from a README / description text.
     *
     * @param readmeContent the raw README text
     * @return list of extracted skill names, never null
     */
    public List<String> extractSkillsFromReadme(String readmeContent) {
        if (readmeContent == null || readmeContent.isBlank()) {
            return List.of();
        }

        String truncated = readmeContent.length() > MAX_INPUT_CHARS
                ? readmeContent.substring(0, MAX_INPUT_CHARS)
                : readmeContent;

        if (!llmClient.isAvailable()) {
            log.debug("LLM unavailable — using catalog + regex skill extraction");
            return extractSkillsWithoutLLM(truncated);
        }

        try {
            List<String> parsed = parseSkillsFromJson(
                    llmClient.chatCompletion(SYSTEM_PROMPT, truncated));
            if (!parsed.isEmpty()) {
                return parsed;
            }
            log.debug("LLM returned no usable skills — falling back to local extraction");
        } catch (Exception e) {
            log.error("LLM skill extraction failed: {}", e.getMessage());
        }

        return extractSkillsWithoutLLM(truncated);
    }

    /**
     * Local extraction: union of seeded-catalog matches and curated regex matches.
     * Catalog matches come first, since they are guaranteed to be real taxonomy entries.
     */
    List<String> extractSkillsWithoutLLM(String text) {
        List<String> combined = new ArrayList<>(extractSkillsFromCatalog(text));
        combined.addAll(extractSkillsWithRegex(text));
        return dedupeAndCap(combined);
    }

    /**
     * Scan text for any skill name present in the seeded catalog.
     *
     * <p>Skipped for names shorter than three characters unless they contain a
     * symbol (so "C#" and "F#" are matched but bare "C", "R" and "Go" are not —
     * those appear in prose far too often to be meaningful).
     */
    List<String> extractSkillsFromCatalog(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<String> found = new ArrayList<>();
        for (TechPattern entry : loadCatalog()) {
            if (entry.pattern().matcher(text).find()) {
                found.add(entry.canonicalName());
            }
        }
        return found;
    }

    /**
     * Curated regex extraction for common technologies and their spelling variants.
     */
    List<String> extractSkillsWithRegex(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<String> found = new ArrayList<>();
        for (TechPattern entry : TECH_PATTERNS) {
            if (entry.pattern().matcher(text).find()) {
                found.add(entry.canonicalName());
            }
        }

        log.debug("Regex extraction found {} skills", found.size());
        return found;
    }

    /**
     * Parse a JSON array string into a list of skill names.
     * Tolerates markdown code fences and surrounding prose from the LLM.
     */
    List<String> parseSkillsFromJson(String jsonResponse) {
        if (jsonResponse == null || jsonResponse.isBlank()) {
            return List.of();
        }

        String cleaned = jsonResponse.trim();
        int start = cleaned.indexOf('[');
        int end = cleaned.lastIndexOf(']');
        if (start < 0 || end <= start) {
            return List.of();
        }
        cleaned = cleaned.substring(start, end + 1);

        try {
            List<String> raw = OBJECT_MAPPER.readValue(cleaned, new TypeReference<List<String>>() {});
            return dedupeAndCap(raw);
        } catch (Exception e) {
            log.warn("Failed to parse LLM skill response: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Load the seeded skill catalog as matchable patterns, caching for
     * {@value #CATALOG_TTL_MILLIS} ms so a bulk GitHub sync issues one query.
     */
    private List<TechPattern> loadCatalog() {
        List<TechPattern> cached = catalogCache;
        if (cached != null && System.currentTimeMillis() - catalogLoadedAt < CATALOG_TTL_MILLIS) {
            return cached;
        }

        List<TechPattern> built = new ArrayList<>();
        try {
            for (Skill skill : skillRepository.findAll()) {
                String name = skill.getName();
                if (!isMatchable(name)) {
                    continue;
                }
                built.add(new TechPattern(catalogPattern(name), name.trim()));
            }
        } catch (Exception e) {
            log.warn("Could not load skill catalog for extraction: {}", e.getMessage());
            return cached != null ? cached : List.of();
        }

        catalogCache = built;
        catalogLoadedAt = System.currentTimeMillis();
        log.debug("Loaded {} matchable skills from catalog", built.size());
        return built;
    }

    /**
     * @return true if a catalog name is specific enough to scan for in free text
     */
    private boolean isMatchable(String name) {
        if (name == null) {
            return false;
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty() || trimmed.length() > 100) {
            return false;
        }
        if (AMBIGUOUS_NAMES.contains(trimmed.toLowerCase(Locale.ROOT))) {
            return false;
        }
        boolean hasSymbol = !trimmed.matches("[A-Za-z0-9 ]+");
        return trimmed.length() >= 3 || hasSymbol;
    }

    /**
     * Build a case-insensitive whole-token pattern for a literal skill name.
     *
     * <p>Uses lookaround guards rather than {@code \b}, because {@code \b} fails
     * for names ending in a non-word character — {@code \bC\+\+\b} never matches
     * "C++" since there is no word boundary after the final '+'.
     */
    private Pattern catalogPattern(String name) {
        String literal = Pattern.quote(name.trim());
        return Pattern.compile(
                "(?<![A-Za-z0-9_+#.-])" + literal + "(?![A-Za-z0-9_+#])",
                Pattern.CASE_INSENSITIVE);
    }

    /**
     * Trim, drop blanks/over-long entries, de-duplicate case-insensitively
     * while preserving first-seen order, and cap at {@value #MAX_SKILLS}.
     */
    private List<String> dedupeAndCap(List<String> names) {
        Map<String, String> unique = new LinkedHashMap<>();
        for (String name : names) {
            if (name == null) {
                continue;
            }
            String trimmed = name.trim();
            if (trimmed.isEmpty() || trimmed.length() > 100) {
                continue;
            }
            unique.putIfAbsent(trimmed.toLowerCase(Locale.ROOT), trimmed);
            if (unique.size() >= MAX_SKILLS) {
                break;
            }
        }
        return new ArrayList<>(unique.values());
    }

    private static TechPattern tech(String regex, String canonicalName) {
        return new TechPattern(Pattern.compile(regex, Pattern.CASE_INSENSITIVE), canonicalName);
    }

    /** A compiled detector paired with the canonical skill name it implies. */
    private record TechPattern(Pattern pattern, String canonicalName) {
    }
}
