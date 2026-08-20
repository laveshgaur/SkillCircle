package com.skillcircle.matching.util;

import com.skillcircle.matching.dto.MatchResponse;

import java.util.*;

/**
 * Evaluation metrics for the matching engine quality.
 * Used for offline evaluation and A/B testing.
 *
 * Implements Precision@K and NDCG@K as specified in the LLD.
 */
public final class MatchMetrics {

    private MatchMetrics() {} // Utility class

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
}
