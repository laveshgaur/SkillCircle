package com.skillcircle.matching.controller;

import com.skillcircle.common.dto.ApiResponse;
import com.skillcircle.matching.dto.EvaluationReport;
import com.skillcircle.matching.evaluation.EvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Admin-only REST endpoint for running the matching pipeline evaluation.
 *
 * Uses synthetic data — no external services (OpenAI, Qdrant) required.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/match/evaluate")
@RequiredArgsConstructor
@Tag(name = "Evaluation", description = "Matching pipeline evaluation and benchmarking")
public class EvaluationController {

    private final EvaluationService evaluationService;

    private volatile EvaluationReport cachedReport;

    @PostMapping
    @Operation(summary = "Run the full evaluation benchmark suite",
            description = "Generates synthetic data and evaluates the matching pipeline " +
                          "under 6 configurations. Returns a comparative report with " +
                          "Precision@K, NDCG@K, MAP@K, MRR, Coverage, and Diversity.")
    public ResponseEntity<ApiResponse<EvaluationReport>> runEvaluation(
            @RequestParam(defaultValue = "50") int userCount,
            @RequestParam(defaultValue = "42") long seed) {

        log.info("Evaluation requested: {} users, seed={}", userCount, seed);
        int clampedCount = Math.min(Math.max(userCount, 10), 200);

        EvaluationReport report = evaluationService.runEvaluation(clampedCount, seed);
        cachedReport = report;

        return ResponseEntity.ok(ApiResponse.success(
                "Evaluation complete. Best config: " + report.getBestConfiguration(),
                report));
    }

    @GetMapping("/report")
    @Operation(summary = "Get the latest evaluation report",
            description = "Returns the most recent evaluation report, or runs a fresh one if none exists.")
    public ResponseEntity<ApiResponse<EvaluationReport>> getReport() {
        if (cachedReport == null) {
            cachedReport = evaluationService.runEvaluation();
        }
        return ResponseEntity.ok(ApiResponse.success(cachedReport));
    }
}
