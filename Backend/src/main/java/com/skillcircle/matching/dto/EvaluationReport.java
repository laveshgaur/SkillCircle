package com.skillcircle.matching.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * Top-level evaluation report returned by the benchmark endpoint.
 * Contains results for each pipeline configuration.
 */
@Data
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EvaluationReport {

    private Instant generatedAt;
    private int totalSyntheticUsers;
    private int queriesRun;

    /** Benchmark results for each pipeline configuration. */
    private List<BenchmarkResult> benchmarks;

    /** Summary: which configuration performed best on NDCG@10? */
    private String bestConfiguration;

    /** Recommended weight values based on evaluation. */
    private WeightConfig recommendedWeights;
}
