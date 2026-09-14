package com.skillcircle.matching.util;

import com.skillcircle.matching.dto.MatchResponse;

import java.util.*;

/**
 * Evaluation metrics for the matching engine quality.
 * Used for offline evaluation, A/B testing, and benchmark comparisons.
 *
 * Metrics implemented:
 * - Precision@K   — fraction of top-K results that are relevant
 * - NDCG@K        — normalized discounted cumulative gain (ranking quality)
 * - MAP@K         — mean average precision across multiple queries
 * - MRR           — mean reciprocal rank (how quickly first relevant result appears)
 * - Coverage      — fraction of users who receive at least one relevant match
 * - Diversity     — variety of attributes (goal types, experience) in recommendations
 */
public final class MatchMetrics {

    private MatchMetrics() {} // Utility class

    // ======================== Precision@K ========================

    /**
     * Precision@K: fraction of top-K results that are relevant.
     *
     * @param results  match results ordered by score
     * @param relevant set of known-relevant user IDs
     * @param k        cutoff
     * @return precision score [0.0, 1.0]
     */
    public static double precisionAtK(List<MatchResponse> results, Set<UUID> relevant, int k) {
        if (k <= 0 || results.isEmpty()) return 0.0;

        long hits = results.stream()
                .limit(k)
                .filter(r -> relevant.contains(r.getUserId()))
                .count();
        return (double) hits / k;
    }

    // ======================== NDCG@K ========================

    /**
     * NDCG@K: Normalized Discounted Cumulative Gain.
     * Measures ranking quality — higher is better.
     *
     * @param results         match results ordered by score
     * @param relevanceScores map of userId → relevance score (e.g., 0-3)
     * @param k               cutoff
     * @return NDCG score [0.0, 1.0]
     */
    public static double ndcgAtK(List<MatchResponse> results, Map<UUID, Double> relevanceScores, int k) {
        if (k <= 0 || results.isEmpty()) return 0.0;

        // DCG
        double dcg = 0.0;
        for (int i = 0; i < Math.min(k, results.size()); i++) {
            double rel = relevanceScores.getOrDefault(results.get(i).getUserId(), 0.0);
            dcg += (Math.pow(2, rel) - 1) / (Math.log(i + 2) / Math.log(2));
        }

        // Ideal DCG
        List<Double> idealRels = relevanceScores.values().stream()
                .sorted(Comparator.reverseOrder())
                .limit(k)
                .toList();
        double idcg = 0.0;
        for (int i = 0; i < idealRels.size(); i++) {
            idcg += (Math.pow(2, idealRels.get(i)) - 1) / (Math.log(i + 2) / Math.log(2));
        }

        return idcg == 0 ? 0.0 : dcg / idcg;
    }

    // ======================== MAP@K ========================

    /**
     * Average Precision@K for a single query.
     * AP@K = (1/min(|relevant|, K)) × Σ_{i=1..K} Precision@i × rel(i)
     *
     * @param results  ranked results for one query
     * @param relevant set of relevant user IDs
     * @param k        cutoff
     * @return AP score [0.0, 1.0]
     */
    public static double averagePrecisionAtK(List<MatchResponse> results, Set<UUID> relevant, int k) {
        if (k <= 0 || results.isEmpty() || relevant.isEmpty()) return 0.0;

        double sumPrecision = 0.0;
        int relevantFound = 0;

        for (int i = 0; i < Math.min(k, results.size()); i++) {
            if (relevant.contains(results.get(i).getUserId())) {
                relevantFound++;
                sumPrecision += (double) relevantFound / (i + 1);
            }
        }

        return relevantFound == 0 ? 0.0 : sumPrecision / Math.min(relevant.size(), k);
    }

    /**
     * Mean Average Precision@K across multiple queries.
     *
     * @param allResults  list of per-query result lists
     * @param allRelevant list of per-query relevant sets
     * @param k           cutoff
     * @return MAP score [0.0, 1.0]
     */
    public static double mapAtK(
            List<List<MatchResponse>> allResults,
            List<Set<UUID>> allRelevant,
            int k) {
        if (allResults.isEmpty()) return 0.0;

        double sumAP = 0.0;
        for (int q = 0; q < allResults.size(); q++) {
            sumAP += averagePrecisionAtK(allResults.get(q), allRelevant.get(q), k);
        }
        return sumAP / allResults.size();
    }

    // ======================== MRR ========================

    /**
     * Reciprocal Rank for a single query: 1 / rank of the first relevant result.
     *
     * @param results  ranked results
     * @param relevant set of relevant user IDs
     * @return RR score [0.0, 1.0]
     */
    public static double reciprocalRank(List<MatchResponse> results, Set<UUID> relevant) {
        for (int i = 0; i < results.size(); i++) {
            if (relevant.contains(results.get(i).getUserId())) {
                return 1.0 / (i + 1);
            }
        }
        return 0.0;
    }

    /**
     * Mean Reciprocal Rank across multiple queries.
     *
     * @param allResults  list of per-query result lists
     * @param allRelevant list of per-query relevant sets
     * @return MRR score [0.0, 1.0]
     */
    public static double mrr(
            List<List<MatchResponse>> allResults,
            List<Set<UUID>> allRelevant) {
        if (allResults.isEmpty()) return 0.0;

        double sumRR = 0.0;
        for (int q = 0; q < allResults.size(); q++) {
            sumRR += reciprocalRank(allResults.get(q), allRelevant.get(q));
        }
        return sumRR / allResults.size();
    }

    // ======================== Coverage ========================

    /**
     * Coverage: fraction of queries that returned at least one relevant result.
     *
     * @param allResults  per-query result lists
     * @param allRelevant per-query relevant sets
     * @param k           cutoff (only check top-K)
     * @return coverage [0.0, 1.0]
     */
    public static double coverage(
            List<List<MatchResponse>> allResults,
            List<Set<UUID>> allRelevant,
            int k) {
        if (allResults.isEmpty()) return 0.0;

        long covered = 0;
        for (int q = 0; q < allResults.size(); q++) {
            final int idx = q;
            boolean hasRelevant = allResults.get(idx).stream()
                    .limit(k)
                    .anyMatch(r -> allRelevant.get(idx).contains(r.getUserId()));
            if (hasRelevant) covered++;
        }
        return (double) covered / allResults.size();
    }

    // ======================== Diversity ========================

    /**
     * Diversity: measures variety of goal types in the top-K results.
     * Returns the number of distinct goal types divided by the number of results (up to K).
     *
     * @param results ranked results
     * @param k       cutoff
     * @return diversity [0.0, 1.0]
     */
    public static double goalTypeDiversity(List<MatchResponse> results, int k) {
        if (results.isEmpty() || k <= 0) return 0.0;

        long count = Math.min(k, results.size());
        long distinctGoals = results.stream()
                .limit(k)
                .map(MatchResponse::getGoalType)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        // Normalize: at most 4 goal types (LEARNING, BUILDING, MENTORING, EXPLORING)
        return Math.min(1.0, (double) distinctGoals / Math.min(count, 4));
    }

    /**
     * Experience level diversity in top-K results.
     *
     * @param results ranked results
     * @param k       cutoff
     * @return diversity [0.0, 1.0]
     */
    public static double experienceDiversity(List<MatchResponse> results, int k) {
        if (results.isEmpty() || k <= 0) return 0.0;

        long count = Math.min(k, results.size());
        long distinct = results.stream()
                .limit(k)
                .map(MatchResponse::getExperienceLevel)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        // At most 4 levels (BEGINNER, INTERMEDIATE, ADVANCED, EXPERT)
        return Math.min(1.0, (double) distinct / Math.min(count, 4));
    }
}
