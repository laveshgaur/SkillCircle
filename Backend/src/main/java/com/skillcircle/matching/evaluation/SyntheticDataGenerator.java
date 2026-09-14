package com.skillcircle.matching.evaluation;

import com.skillcircle.matching.dto.MatchResponse;
import com.skillcircle.matching.dto.ScoredCandidate;
import com.skillcircle.profile.entity.GoalType;
import lombok.Builder;
import lombok.Data;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates synthetic user datasets and ground truth relevance judgments
 * for offline evaluation of the matching pipeline.
 *
 * Creates 50 synthetic users across 7 skill categories and 4 goal types,
 * with pre-computed collaboration edges for warm-start scenarios and
 * ground truth relevance based on skill overlap, goal alignment, and timezone.
 */
public final class SyntheticDataGenerator {

    private SyntheticDataGenerator() {}

    // ---- Skill pools by category ----
    private static final Map<String, List<String>> SKILL_POOLS = Map.of(
        "BACKEND",   List.of("Java", "Spring Boot", "PostgreSQL", "Redis", "Kafka", "Go", "Rust", "Python", "Node.js", "GraphQL"),
        "FRONTEND",  List.of("React", "TypeScript", "CSS", "Vue", "Angular", "Svelte", "Next.js", "Tailwind", "HTML5", "Figma"),
        "DEVOPS",    List.of("Docker", "Kubernetes", "AWS", "Terraform", "CI/CD", "Linux", "Nginx", "Prometheus", "Grafana", "Ansible"),
        "DATA",      List.of("Python", "Pandas", "SQL", "Spark", "Airflow", "dbt", "Snowflake", "Tableau", "R", "Jupyter"),
        "ML",        List.of("PyTorch", "TensorFlow", "Scikit-learn", "NLP", "Computer Vision", "MLflow", "Hugging Face", "LangChain", "ONNX", "OpenAI"),
        "MOBILE",    List.of("Swift", "Kotlin", "React Native", "Flutter", "iOS", "Android", "Jetpack Compose", "SwiftUI", "Expo", "Firebase"),
        "SECURITY",  List.of("OWASP", "Penetration Testing", "Cryptography", "OAuth2", "JWT", "HashiCorp Vault", "SOC2", "ISO 27001", "Burp Suite", "Wireshark")
    );

    private static final String[] CATEGORIES = SKILL_POOLS.keySet().toArray(new String[0]);
    private static final GoalType[] GOALS = GoalType.values();
    private static final String[] EXPERIENCE_LEVELS = {"BEGINNER", "INTERMEDIATE", "ADVANCED", "EXPERT"};
    private static final String[] TIMEZONES = {"UTC-8", "UTC-5", "UTC+0", "UTC+1", "UTC+3", "UTC+5:30", "UTC+8", "UTC+9"};
    private static final String[] AVAILABILITIES = {"OPEN", "SELECTIVE", "BUSY"};

    /**
     * Generate a dataset of synthetic users.
     *
     * @param count number of synthetic users to generate
     * @param seed  random seed for reproducibility
     * @return dataset with users, collab edges, and ground truth
     */
    public static SyntheticDataset generate(int count, long seed) {
        Random rng = new Random(seed);
        List<SyntheticUser> users = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String primaryCategory = CATEGORIES[i % CATEGORIES.length];
            String secondaryCategory = CATEGORIES[(i + 3) % CATEGORIES.length];

            // Pick 3-6 skills from primary + 1-3 from secondary
            List<String> skills = new ArrayList<>();
            skills.addAll(pickRandom(SKILL_POOLS.get(primaryCategory), 3 + rng.nextInt(4), rng));
            skills.addAll(pickRandom(SKILL_POOLS.get(secondaryCategory), 1 + rng.nextInt(3), rng));

            SyntheticUser user = SyntheticUser.builder()
                    .id(UUID.nameUUIDFromBytes(("synthetic-user-" + i).getBytes()))
                    .username("user_" + i)
                    .displayName("Synthetic User " + i)
                    .bio("Developer specializing in " + primaryCategory.toLowerCase() + " with interest in " + secondaryCategory.toLowerCase())
                    .goalType(GOALS[i % GOALS.length])
                    .experienceLevel(EXPERIENCE_LEVELS[i % EXPERIENCE_LEVELS.length])
                    .timezone(TIMEZONES[i % TIMEZONES.length])
                    .availability(AVAILABILITIES[rng.nextInt(AVAILABILITIES.length)])
                    .skills(skills)
                    .primaryCategory(primaryCategory)
                    .isColdStart(i % 5 == 0) // Every 5th user is cold-start (no collab edges)
                    .build();

            users.add(user);
        }

        // Generate collaboration edges for warm-start users
        List<CollabEdge> edges = generateCollabEdges(users, rng);

        // Generate ground truth relevance judgments
        Map<UUID, Map<UUID, Double>> groundTruth = computeGroundTruth(users);

        return SyntheticDataset.builder()
                .users(users)
                .collabEdges(edges)
                .groundTruth(groundTruth)
                .build();
    }

    /**
     * Compute ground truth relevance score between all user pairs.
     * Score based on: skill overlap (Jaccard), goal alignment, timezone proximity.
     */
    static Map<UUID, Map<UUID, Double>> computeGroundTruth(List<SyntheticUser> users) {
        Map<UUID, Map<UUID, Double>> truth = new HashMap<>();

        for (SyntheticUser query : users) {
            Map<UUID, Double> relevance = new HashMap<>();
            for (SyntheticUser candidate : users) {
                if (query.getId().equals(candidate.getId())) continue;

                double skillScore = jaccardSimilarity(
                        new HashSet<>(query.getSkills()),
                        new HashSet<>(candidate.getSkills()));

                double goalScore = goalAlignmentScore(query.getGoalType(), candidate.getGoalType());

                double tzScore = timezoneProximity(query.getTimezone(), candidate.getTimezone());

                // Weighted relevance: 0.5 skill + 0.3 goal + 0.2 timezone
                double relevanceScore = 0.5 * skillScore + 0.3 * goalScore + 0.2 * tzScore;

                // Scale to 0-3 range for NDCG (0=irrelevant, 1=marginal, 2=relevant, 3=highly relevant)
                double scaledRelevance = Math.min(3.0, relevanceScore * 4.0);
                relevance.put(candidate.getId(), scaledRelevance);
            }
            truth.put(query.getId(), relevance);
        }
        return truth;
    }

    /**
     * Generate collaboration edges (co-contribution relationships) between warm-start users.
     */
    static List<CollabEdge> generateCollabEdges(List<SyntheticUser> users, Random rng) {
        List<CollabEdge> edges = new ArrayList<>();
        List<SyntheticUser> warmUsers = users.stream()
                .filter(u -> !u.isColdStart())
                .toList();

        for (int i = 0; i < warmUsers.size(); i++) {
            // Each warm user has 2-5 collaboration edges
            int edgeCount = 2 + rng.nextInt(4);
            for (int e = 0; e < edgeCount && e < warmUsers.size() - 1; e++) {
                int partnerIdx = (i + 1 + rng.nextInt(Math.max(1, warmUsers.size() - 1))) % warmUsers.size();
                if (partnerIdx == i) continue;

                SyntheticUser a = warmUsers.get(i);
                SyntheticUser b = warmUsers.get(partnerIdx);

                // Strength correlated with skill overlap
                double overlap = jaccardSimilarity(new HashSet<>(a.getSkills()), new HashSet<>(b.getSkills()));
                int strength = Math.max(1, (int)(overlap * 10));

                edges.add(CollabEdge.builder()
                        .userAId(a.getId())
                        .userBId(b.getId())
                        .strength(strength)
                        .type("CO_CONTRIBUTOR")
                        .build());
            }
        }
        return edges;
    }

    // ---- Similarity functions ----

    static double jaccardSimilarity(Set<String> a, Set<String> b) {
        if (a.isEmpty() && b.isEmpty()) return 0.0;
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return (double) intersection.size() / union.size();
    }

    static double goalAlignmentScore(GoalType a, GoalType b) {
        if (a == null || b == null) return 0.5;
        if (a == b) return 1.0;
        if ((a == GoalType.LEARNING && b == GoalType.MENTORING) ||
            (a == GoalType.MENTORING && b == GoalType.LEARNING)) return 0.8;
        if ((a == GoalType.BUILDING && b == GoalType.EXPLORING) ||
            (a == GoalType.EXPLORING && b == GoalType.BUILDING)) return 0.5;
        return 0.2;
    }

    static double timezoneProximity(String tz1, String tz2) {
        int offset1 = parseOffset(tz1);
        int offset2 = parseOffset(tz2);
        int diff = Math.abs(offset1 - offset2);
        if (diff > 12) diff = 24 - diff; // wrap around
        return Math.max(0.0, 1.0 - diff / 12.0);
    }

    private static int parseOffset(String tz) {
        if (tz == null) return 0;
        try {
            String num = tz.replace("UTC", "").replace("+", "");
            if (num.contains(":")) {
                String[] parts = num.split(":");
                return Integer.parseInt(parts[0]);
            }
            return Integer.parseInt(num);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static <T> List<T> pickRandom(List<T> source, int count, Random rng) {
        List<T> shuffled = new ArrayList<>(source);
        Collections.shuffle(shuffled, rng);
        return shuffled.subList(0, Math.min(count, shuffled.size()));
    }

    /**
     * Convert a SyntheticUser to a MatchResponse for evaluation input.
     */
    public static MatchResponse toMatchResponse(SyntheticUser user, double score) {
        return MatchResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .bio(user.getBio())
                .goalType(user.getGoalType() != null ? user.getGoalType().name() : null)
                .experienceLevel(user.getExperienceLevel())
                .timezone(user.getTimezone())
                .skills(user.getSkills())
                .finalScore(score)
                .build();
    }

    /**
     * Get the set of relevant user IDs for a query user at a given threshold.
     * Relevance ≥ 1.5 (scaled) is considered relevant for Precision/MAP.
     */
    public static Set<UUID> getRelevantSet(Map<UUID, Double> truthMap, double threshold) {
        return truthMap.entrySet().stream()
                .filter(e -> e.getValue() >= threshold)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    // ---- Data classes ----

    @Data
    @Builder
    public static class SyntheticUser {
        private UUID id;
        private String username;
        private String displayName;
        private String bio;
        private GoalType goalType;
        private String experienceLevel;
        private String timezone;
        private String availability;
        private List<String> skills;
        private String primaryCategory;
        private boolean isColdStart;
    }

    @Data
    @Builder
    public static class CollabEdge {
        private UUID userAId;
        private UUID userBId;
        private int strength;
        private String type;
    }

    @Data
    @Builder
    public static class SyntheticDataset {
        private List<SyntheticUser> users;
        private List<CollabEdge> collabEdges;
        private Map<UUID, Map<UUID, Double>> groundTruth;

        public List<SyntheticUser> getColdStartUsers() {
            return users.stream().filter(SyntheticUser::isColdStart).toList();
        }

        public List<SyntheticUser> getWarmStartUsers() {
            return users.stream().filter(u -> !u.isColdStart()).toList();
        }
    }
}
