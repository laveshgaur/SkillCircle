package com.skillcircle.matching.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * Internal DTO representing a candidate during the matching pipeline.
 * Carries per-stage scores through the 3-stage process.
 */
@Data
@Builder
@AllArgsConstructor
public class ScoredCandidate {

    private UUID userId;
    private double semanticScore; // Stage 1: cosine similarity from vector DB
    private double collabScore;   // Stage 2: collaboration graph boost
    private double goalScore;     // Stage 2: goal type alignment
    private double finalScore;    // Weighted combination
}
