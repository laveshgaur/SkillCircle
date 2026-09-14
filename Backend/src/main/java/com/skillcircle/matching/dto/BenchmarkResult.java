package com.skillcircle.matching.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * Metrics for one pipeline configuration within a benchmark evaluation.
 */
@Data
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BenchmarkResult {

    /** Name of the configuration (e.g., "Semantic-Only", "Hybrid", "Cold-Start", "Warm-Start"). */
    private String configName;

    /** Weight configuration used. */
    private WeightConfig weights;

    /** Number of queries evaluated. */
    private int queriesEvaluated;

    /** Whether collaboration edges were present. */
    private boolean hasCollabEdges;

    // --- Core Metrics ---

    private double precisionAt5;
    private double precisionAt10;
    private double ndcgAt5;
    private double ndcgAt10;
    private double mapAt10;
    private double mrr;
    private double coverage;
    private double goalTypeDiversity;
    private double experienceDiversity;

    /** Average number of results per query. */
    private double avgResultsPerQuery;
}
