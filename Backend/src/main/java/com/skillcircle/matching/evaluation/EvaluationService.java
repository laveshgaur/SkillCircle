package com.skillcircle.matching.evaluation;

import com.skillcircle.matching.dto.*;
import com.skillcircle.matching.evaluation.SyntheticDataGenerator.*;
import com.skillcircle.matching.util.MatchMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Offline evaluation service for the matching pipeline.
 *
 * Generates synthetic data and simulates the 3-stage pipeline under
 * different weight configurations to produce comparative benchmarks:
 *
 *   1. Semantic-Only (α=1.0, β=0.0, γ=0.0) — baseline
 *   2. Hybrid Default (α=0.5, β=0.3, γ=0.2) — production config
 *   3. Cold-Start cohort (no collab edges, β → 0)
 *   4. Warm-Start cohort (with collab edges)
 *
 * The evaluation uses synthetic ground truth rather than the real
 * embedding pipeline so it can run without external services (OpenAI, Qdrant).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationService {

    private static final int DEFAULT_USER_COUNT = 50;
    private static final long DEFAULT_SEED = 42L;
    private static final double RELEVANCE_THRESHOLD = 1.5; // scaled 0-3

    /**
     * Run the full evaluation benchmark suite.
     */
    public EvaluationReport runEvaluation() {
        return runEvaluation(DEFAULT_USER_COUNT, DEFAULT_SEED);
    }

    /**
     * Run evaluation with custom parameters.
     */
    public EvaluationReport runEvaluation(int userCount, long seed) {
        log.info("Starting evaluation with {} synthetic users (seed={})", userCount, seed);

        SyntheticDataset dataset = SyntheticDataGenerator.generate(userCount, seed);
        List<SyntheticUser> allUsers = dataset.getUsers();
        Map<UUID, Map<UUID, Double>> groundTruth = dataset.getGroundTruth();

        List<BenchmarkResult> benchmarks = new ArrayList<>();

        // ========== Config 1: Semantic-Only Baseline ==========
        benchmarks.add(runBenchmark(
                "Semantic-Only (Baseline)",
                WeightConfig.semanticOnly(),
                allUsers, allUsers, groundTruth,
                Collections.emptyList(), false
        ));

        // ========== Config 2: Hybrid Default ==========
        benchmarks.add(runBenchmark(
                "Hybrid (Default α=0.5, β=0.3, γ=0.2)",
                WeightConfig.hybrid(),
                allUsers, allUsers, groundTruth,
                dataset.getCollabEdges(), true
        ));

        // ========== Config 3: Cold-Start Cohort ==========
        List<SyntheticUser> coldUsers = dataset.getColdStartUsers();
        if (!coldUsers.isEmpty()) {
            benchmarks.add(runBenchmark(
                    "Cold-Start Cohort",
                    WeightConfig.hybrid(),
                    coldUsers, allUsers, groundTruth,
                    Collections.emptyList(), false
            ));
        }

        // ========== Config 4: Warm-Start Cohort ==========
        List<SyntheticUser> warmUsers = dataset.getWarmStartUsers();
        if (!warmUsers.isEmpty()) {
            benchmarks.add(runBenchmark(
                    "Warm-Start Cohort",
                    WeightConfig.hybrid(),
                    warmUsers, allUsers, groundTruth,
                    dataset.getCollabEdges(), true
            ));
        }

        // ========== Config 5: Goal-Heavy ==========
        benchmarks.add(runBenchmark(
                "Goal-Heavy (α=0.3, β=0.2, γ=0.5)",
                WeightConfig.goalHeavy(),
                allUsers, allUsers, groundTruth,
                dataset.getCollabEdges(), true
        ));

        // ========== Config 6: Balanced ==========
        benchmarks.add(runBenchmark(
                "Balanced (α=0.34, β=0.33, γ=0.33)",
                WeightConfig.balanced(),
                allUsers, allUsers, groundTruth,
                dataset.getCollabEdges(), true
        ));

        // Determine best configuration by NDCG@10
        String bestConfig = benchmarks.stream()
                .max(Comparator.comparingDouble(BenchmarkResult::getNdcgAt10))
                .map(BenchmarkResult::getConfigName)
                .orElse("Unknown");

        // Recommend weights from the best-performing config
        WeightConfig recommended = benchmarks.stream()
                .max(Comparator.comparingDouble(BenchmarkResult::getNdcgAt10))
                .map(BenchmarkResult::getWeights)
                .orElse(WeightConfig.hybrid());

        EvaluationReport report = EvaluationReport.builder()
                .generatedAt(Instant.now())
                .totalSyntheticUsers(userCount)
                .queriesRun(benchmarks.stream().mapToInt(BenchmarkResult::getQueriesEvaluated).sum())
                .benchmarks(benchmarks)
                .bestConfiguration(bestConfig)
                .recommendedWeights(recommended)
                .build();

        log.info("Evaluation complete. Best config: {} (NDCG@10={})",
                bestConfig,
                benchmarks.stream()
                        .max(Comparator.comparingDouble(BenchmarkResult::getNdcgAt10))
                        .map(b -> String.format("%.4f", b.getNdcgAt10()))
                        .orElse("N/A"));

        return report;
    }

    /**
     * Run a single benchmark configuration.
     *
     * @param configName   display name
     * @param weights      pipeline weights
     * @param queryUsers   users to issue queries for
     * @param candidatePool all candidate users
     * @param groundTruth  ground truth relevance
     * @param collabEdges  collaboration edges (empty for cold-start)
     * @param hasCollab    whether collab scores should be simulated
     */
    private BenchmarkResult runBenchmark(
            String configName,
            WeightConfig weights,
            List<SyntheticUser> queryUsers,
            List<SyntheticUser> candidatePool,
            Map<UUID, Map<UUID, Double>> groundTruth,
            List<CollabEdge> collabEdges,
            boolean hasCollab) {

        // Build collab score lookup
        Map<UUID, Map<UUID, Double>> collabScoreMap = buildCollabScoreMap(collabEdges);

        List<List<MatchResponse>> allResults = new ArrayList<>();
        List<Set<UUID>> allRelevant = new ArrayList<>();
        List<Map<UUID, Double>> allRelevanceScores = new ArrayList<>();

        for (SyntheticUser queryUser : queryUsers) {
            Map<UUID, Double> truthMap = groundTruth.getOrDefault(queryUser.getId(), Map.of());
            if (truthMap.isEmpty()) continue;

            // Simulate the 3-stage pipeline
            List<MatchResponse> results = simulatePipeline(
                    queryUser, candidatePool, truthMap,
                    collabScoreMap.getOrDefault(queryUser.getId(), Map.of()),
                    weights, hasCollab
            );

            Set<UUID> relevant = SyntheticDataGenerator.getRelevantSet(truthMap, RELEVANCE_THRESHOLD);

            allResults.add(results);
            allRelevant.add(relevant);
            allRelevanceScores.add(truthMap);
        }

        if (allResults.isEmpty()) {
            return BenchmarkResult.builder()
                    .configName(configName)
                    .weights(weights)
                    .queriesEvaluated(0)
                    .hasCollabEdges(hasCollab)
                    .build();
        }

        // Compute metrics
        double p5 = avgMetric(allResults, allRelevant, (r, rel) -> MatchMetrics.precisionAtK(r, rel, 5));
        double p10 = avgMetric(allResults, allRelevant, (r, rel) -> MatchMetrics.precisionAtK(r, rel, 10));
        double n5 = avgNdcg(allResults, allRelevanceScores, 5);
        double n10 = avgNdcg(allResults, allRelevanceScores, 10);
        double map10 = MatchMetrics.mapAtK(allResults, allRelevant, 10);
        double mrrScore = MatchMetrics.mrr(allResults, allRelevant);
        double coverageScore = MatchMetrics.coverage(allResults, allRelevant, 10);

        // Diversity (average across all result lists)
        double goalDiv = allResults.stream()
                .mapToDouble(r -> MatchMetrics.goalTypeDiversity(r, 10))
                .average().orElse(0.0);
        double expDiv = allResults.stream()
                .mapToDouble(r -> MatchMetrics.experienceDiversity(r, 10))
                .average().orElse(0.0);

        double avgResults = allResults.stream()
                .mapToInt(List::size)
                .average().orElse(0.0);

        return BenchmarkResult.builder()
                .configName(configName)
                .weights(weights)
                .queriesEvaluated(allResults.size())
                .hasCollabEdges(hasCollab)
                .precisionAt5(round(p5))
                .precisionAt10(round(p10))
                .ndcgAt5(round(n5))
                .ndcgAt10(round(n10))
                .mapAt10(round(map10))
                .mrr(round(mrrScore))
                .coverage(round(coverageScore))
                .goalTypeDiversity(round(goalDiv))
                .experienceDiversity(round(expDiv))
                .avgResultsPerQuery(round(avgResults))
                .build();
    }

    /**
     * Simulate the 3-stage matching pipeline without real external services.
     * Uses ground truth similarities as "semantic scores" and collab edge map
     * for collaboration scores.
     */
    private List<MatchResponse> simulatePipeline(
            SyntheticUser queryUser,
            List<SyntheticUser> candidatePool,
            Map<UUID, Double> truthMap,
            Map<UUID, Double> collabScores,
            WeightConfig weights,
            boolean hasCollab) {

        // Stage 1: Semantic retrieval (simulate with skill overlap as "semantic score")
        List<ScoredCandidate> candidates = candidatePool.stream()
                .filter(c -> !c.getId().equals(queryUser.getId()))
                .map(c -> {
                    double skillOverlap = SyntheticDataGenerator.jaccardSimilarity(
                            new HashSet<>(queryUser.getSkills()),
                            new HashSet<>(c.getSkills()));
                    return ScoredCandidate.builder()
                            .userId(c.getId())
                            .semanticScore(skillOverlap)
                            .build();
                })
                .sorted(Comparator.comparingDouble(ScoredCandidate::getSemanticScore).reversed())
                .limit(50) // top-K = 50
                .collect(Collectors.toCollection(ArrayList::new));

        // Stage 2: Collaborative re-ranking
        double effectiveAlpha = hasCollab && !collabScores.isEmpty() ? weights.getAlpha() : weights.getAlpha() + weights.getBeta();
        double effectiveBeta = hasCollab && !collabScores.isEmpty() ? weights.getBeta() : 0.0;

        for (ScoredCandidate cand : candidates) {
            double collab = collabScores.getOrDefault(cand.getUserId(), 0.0);
            double goal = goalScoreForCandidate(queryUser, cand.getUserId(), candidatePool);

            cand.setCollabScore(collab);
            cand.setGoalScore(goal);
            cand.setFinalScore(
                    effectiveAlpha * cand.getSemanticScore()
                    + effectiveBeta * collab
                    + weights.getGamma() * goal
            );
        }

        // Stage 3: Hard filters (simulate: remove BUSY users, keep timezone overlap ≥ 4h)
        candidates.removeIf(c -> {
            SyntheticUser cUser = findUser(candidatePool, c.getUserId());
            if (cUser == null) return true;
            if ("BUSY".equals(cUser.getAvailability())) return true;
            double tzProx = SyntheticDataGenerator.timezoneProximity(queryUser.getTimezone(), cUser.getTimezone());
            return tzProx < 0.33; // < 4h overlap
        });

        // Sort by final score
        candidates.sort(Comparator.comparingDouble(ScoredCandidate::getFinalScore).reversed());

        // Convert to MatchResponse
        return candidates.stream()
                .limit(20)
                .map(c -> {
                    SyntheticUser cUser = findUser(candidatePool, c.getUserId());
                    return SyntheticDataGenerator.toMatchResponse(
                            cUser != null ? cUser : SyntheticUser.builder().id(c.getUserId()).username("unknown").build(),
                            c.getFinalScore());
                })
                .toList();
    }

    private double goalScoreForCandidate(SyntheticUser query, UUID candidateId, List<SyntheticUser> pool) {
        SyntheticUser candidate = findUser(pool, candidateId);
        if (candidate == null) return 0.5;
        return SyntheticDataGenerator.goalAlignmentScore(query.getGoalType(), candidate.getGoalType());
    }

    private SyntheticUser findUser(List<SyntheticUser> pool, UUID id) {
        return pool.stream().filter(u -> u.getId().equals(id)).findFirst().orElse(null);
    }

    /**
     * Build collaboration score map from edges.
     * Normalizes per-user scores to [0, 1].
     */
    private Map<UUID, Map<UUID, Double>> buildCollabScoreMap(List<CollabEdge> edges) {
        Map<UUID, Map<UUID, Double>> map = new HashMap<>();

        for (CollabEdge edge : edges) {
            map.computeIfAbsent(edge.getUserAId(), k -> new HashMap<>())
                    .merge(edge.getUserBId(), (double) edge.getStrength(), Double::sum);
            map.computeIfAbsent(edge.getUserBId(), k -> new HashMap<>())
                    .merge(edge.getUserAId(), (double) edge.getStrength(), Double::sum);
        }

        // Normalize per user
        for (Map<UUID, Double> userScores : map.values()) {
            double max = userScores.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
            if (max > 0) {
                userScores.replaceAll((k, v) -> v / max);
            }
        }

        return map;
    }

    // ---- Helpers ----

    private double avgMetric(List<List<MatchResponse>> allResults, List<Set<UUID>> allRelevant,
                             MetricFn fn) {
        double sum = 0.0;
        for (int i = 0; i < allResults.size(); i++) {
            sum += fn.compute(allResults.get(i), allRelevant.get(i));
        }
        return allResults.isEmpty() ? 0.0 : sum / allResults.size();
    }

    private double avgNdcg(List<List<MatchResponse>> allResults, List<Map<UUID, Double>> allScores, int k) {
        double sum = 0.0;
        for (int i = 0; i < allResults.size(); i++) {
            sum += MatchMetrics.ndcgAtK(allResults.get(i), allScores.get(i), k);
        }
        return allResults.isEmpty() ? 0.0 : sum / allResults.size();
    }

    private static double round(double val) {
        return Math.round(val * 10000.0) / 10000.0;
    }

    @FunctionalInterface
    private interface MetricFn {
        double compute(List<MatchResponse> results, Set<UUID> relevant);
    }
}
