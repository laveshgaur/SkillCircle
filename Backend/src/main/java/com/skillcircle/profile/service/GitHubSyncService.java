package com.skillcircle.profile.service;

import com.skillcircle.ai.service.SkillExtractionService;
import com.skillcircle.auth.entity.User;
import com.skillcircle.exception.BadRequestException;
import com.skillcircle.profile.entity.*;
import com.skillcircle.profile.repository.ProfileRepository;
import com.skillcircle.profile.repository.ProfileSkillRepository;
import com.skillcircle.profile.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Service for syncing a user's GitHub profile data.
 * Fetches public repos and extracts languages and topics as skills.
 *
 * Uses on-demand sync triggered by the user (not automatic),
 * with results cached in the profile for 24 hours.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubSyncService {

    private static final String GITHUB_API = "https://api.github.com";

    /** Max repos whose README is fetched for deep skill extraction, to bound API rate usage. */
    private static final int MAX_README_REPOS = 10;

    private final ProfileRepository profileRepository;
    private final SkillRepository skillRepository;
    private final ProfileSkillRepository profileSkillRepository;
    private final SkillExtractionService skillExtractionService;

    /**
     * Sync GitHub profile data: fetch repos, extract languages/topics,
     * and add them as skills to the user's profile.
     *
     * @return number of new skills added
     */
    @Transactional
    @SuppressWarnings("unchecked")
    public int syncGitHub(User user) {
        Profile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BadRequestException(
                        "Create a profile before syncing GitHub"));

        String githubUsername = profile.getGithubUsername();
        if (githubUsername == null || githubUsername.isBlank()) {
            throw new BadRequestException(
                    "Set your GitHub username in your profile before syncing");
        }

        RestClient restClient = RestClient.builder()
                .baseUrl(GITHUB_API)
                .defaultHeader("Accept", "application/vnd.github.v3+json")
                .defaultHeader("User-Agent", "SkillCircle-App")
                .build();

        // Fetch public repos (up to 100)
        List<Map<String, Object>> repos;
        try {
            repos = restClient.get()
                    .uri("/users/{username}/repos?per_page=100&sort=updated", githubUsername)
                    .retrieve()
                    .body(List.class);
        } catch (Exception e) {
            log.error("GitHub API error for user {}: {}", githubUsername, e.getMessage());
            throw new BadRequestException(
                    "Failed to fetch GitHub repos. Verify your username is correct.");
        }

        if (repos == null || repos.isEmpty()) {
            log.info("No repos found for GitHub user: {}", githubUsername);
            return 0;
        }

        int added = 0;

        // Extract language from each repo
        for (Map<String, Object> repo : repos) {
            String language = (String) repo.get("language");
            if (language != null && !language.isBlank()) {
                added += addSkillIfNotExists(profile, language, SkillCategory.LANGUAGE);
            }

            // Extract topics
            Object topicsObj = repo.get("topics");
            if (topicsObj instanceof List<?> topics) {
                for (Object topic : topics) {
                    if (topic instanceof String topicStr && !topicStr.isBlank()) {
                        added += addSkillIfNotExists(profile, topicStr, SkillCategory.OTHER);
                    }
                }
            }
        }

        // Deep extraction: fetch READMEs for the most recently-updated repos and run
        // them through the AI skill extractor (LLM, with catalog/regex fallback). Bounded
        // to MAX_README_REPOS to stay within GitHub's unauthenticated rate limit (60/hr).
        RestClient rawClient = RestClient.builder()
                .baseUrl(GITHUB_API)
                .defaultHeader("Accept", "application/vnd.github.raw")
                .defaultHeader("User-Agent", "SkillCircle-App")
                .build();
        int readmeLimit = Math.min(repos.size(), MAX_README_REPOS);
        for (int i = 0; i < readmeLimit; i++) {
            String repoName = (String) repos.get(i).get("name");
            if (repoName == null || repoName.isBlank()) {
                continue;
            }
            String readme = fetchReadme(rawClient, githubUsername, repoName);
            if (readme != null && !readme.isBlank()) {
                added += addSkillsFromReadme(profile, readme);
            }
        }

        log.info("GitHub sync for {}: {} new skills added from {} repos",
                githubUsername, added, repos.size());

        return added;
    }

    /**
     * Fetch the raw README text for a repo. Returns null when the repo has no README
     * (404) or the request otherwise fails — never throws, so one bad repo does not
     * abort the whole sync.
     */
    String fetchReadme(RestClient rawClient, String owner, String repo) {
        try {
            return rawClient.get()
                    .uri("/repos/{owner}/{repo}/readme", owner, repo)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.debug("No README for {}/{}: {}", owner, repo, e.getMessage());
            return null;
        }
    }

    /**
     * Run README text through the AI skill extractor and add every discovered skill
     * to the profile (reusing the catalog entry when the name already exists).
     *
     * @return number of new skills added
     */
    int addSkillsFromReadme(Profile profile, String readme) {
        int added = 0;
        for (String skillName : skillExtractionService.extractSkillsFromReadme(readme)) {
            added += addSkillIfNotExists(profile, skillName, SkillCategory.OTHER);
        }
        return added;
    }

    /**
     * Add a skill to the profile if it doesn't already exist.
     * Creates the skill in the catalog if it's not a known skill.
     *
     * @return 1 if added, 0 if already existed
     */
    private int addSkillIfNotExists(Profile profile, String skillName, SkillCategory category) {
        // Normalize skill name
        String normalized = skillName.trim();
        if (normalized.length() > 100) return 0;

        // Find or create skill in catalog
        Skill skill = skillRepository.findByNameIgnoreCase(normalized)
                .orElseGet(() -> {
                    Skill newSkill = Skill.builder()
                            .name(normalized)
                            .category(category)
                            .build();
                    return skillRepository.save(newSkill);
                });

        // Check if already in profile
        if (profileSkillRepository.existsByProfileIdAndSkillId(profile.getId(), skill.getId())) {
            return 0;
        }

        // Add to profile
        ProfileSkill ps = ProfileSkill.builder()
                .profileId(profile.getId())
                .skillId(skill.getId())
                .proficiency(Proficiency.INTERMEDIATE)
                .build();
        profileSkillRepository.save(ps);

        return 1;
    }
}
