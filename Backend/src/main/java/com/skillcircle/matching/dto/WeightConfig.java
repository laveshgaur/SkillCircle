package com.skillcircle.matching.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Matching pipeline weight configuration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeightConfig {

    /** Weight for semantic similarity (α). */
    private double alpha;

    /** Weight for collaboration score (β). */
    private double beta;

    /** Weight for goal alignment (γ). */
    private double gamma;

    public static WeightConfig semanticOnly() {
        return new WeightConfig(1.0, 0.0, 0.0);
    }

    public static WeightConfig hybrid() {
        return new WeightConfig(0.5, 0.3, 0.2);
    }

    public static WeightConfig goalHeavy() {
        return new WeightConfig(0.3, 0.2, 0.5);
    }

    public static WeightConfig balanced() {
        return new WeightConfig(0.34, 0.33, 0.33);
    }

    @Override
    public String toString() {
        return String.format("α=%.2f, β=%.2f, γ=%.2f", alpha, beta, gamma);
    }
}
