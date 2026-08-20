package com.skillcircle.matching.util;

import com.skillcircle.matching.dto.MatchResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class MatchMetricsTest {

    @Test
    @DisplayName("Precision@K with perfect relevance")
    void precisionAtK_perfect() {
        UUID u1 = UUID.randomUUID(), u2 = UUID.randomUUID();
        List<MatchResponse> results = List.of(
                MatchResponse.builder().userId(u1).build(),
                MatchResponse.builder().userId(u2).build()
        );
        assertThat(MatchMetrics.precisionAtK(results, Set.of(u1, u2), 2)).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Precision@K with partial relevance")
    void precisionAtK_partial() {
        UUID u1 = UUID.randomUUID(), u2 = UUID.randomUUID();
        List<MatchResponse> results = List.of(
                MatchResponse.builder().userId(u1).build(),
                MatchResponse.builder().userId(u2).build()
        );
        assertThat(MatchMetrics.precisionAtK(results, Set.of(u1), 2)).isEqualTo(0.5);
    }

    @Test
    @DisplayName("NDCG@K with perfect ranking")
    void ndcgAtK_perfect() {
        UUID u1 = UUID.randomUUID(), u2 = UUID.randomUUID();
        List<MatchResponse> results = List.of(
                MatchResponse.builder().userId(u1).build(),
                MatchResponse.builder().userId(u2).build()
        );
        Map<UUID, Double> rel = Map.of(u1, 3.0, u2, 2.0);
        assertThat(MatchMetrics.ndcgAtK(results, rel, 2)).isEqualTo(1.0);
    }

    @Test
    @DisplayName("NDCG@K with swapped ranking < 1.0")
    void ndcgAtK_swapped() {
        UUID u1 = UUID.randomUUID(), u2 = UUID.randomUUID();
        List<MatchResponse> results = List.of(
                MatchResponse.builder().userId(u2).build(),
                MatchResponse.builder().userId(u1).build()
        );
        Map<UUID, Double> rel = Map.of(u1, 3.0, u2, 1.0);
        double ndcg = MatchMetrics.ndcgAtK(results, rel, 2);
        assertThat(ndcg).isLessThan(1.0).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("Edge cases return 0")
    void edgeCases() {
        assertThat(MatchMetrics.precisionAtK(List.of(), Set.of(), 0)).isEqualTo(0.0);
        assertThat(MatchMetrics.ndcgAtK(List.of(), Map.of(), 5)).isEqualTo(0.0);
    }
}
