package com.skillcircle.matching.util;

import com.skillcircle.matching.dto.MatchResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class MatchMetricsTest {

    // ====================== Precision@K ======================

    @Nested
    @DisplayName("Precision@K")
    class PrecisionTests {

        @Test
        @DisplayName("Perfect relevance → 1.0")
        void precisionAtK_perfect() {
            UUID u1 = UUID.randomUUID(), u2 = UUID.randomUUID();
            List<MatchResponse> results = List.of(
                    MatchResponse.builder().userId(u1).build(),
                    MatchResponse.builder().userId(u2).build()
            );
            assertThat(MatchMetrics.precisionAtK(results, Set.of(u1, u2), 2)).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Partial relevance → 0.5")
        void precisionAtK_partial() {
            UUID u1 = UUID.randomUUID(), u2 = UUID.randomUUID();
            List<MatchResponse> results = List.of(
                    MatchResponse.builder().userId(u1).build(),
                    MatchResponse.builder().userId(u2).build()
            );
            assertThat(MatchMetrics.precisionAtK(results, Set.of(u1), 2)).isEqualTo(0.5);
        }

        @Test
        @DisplayName("No relevant results → 0.0")
        void precisionAtK_none() {
            UUID u1 = UUID.randomUUID();
            List<MatchResponse> results = List.of(
                    MatchResponse.builder().userId(u1).build()
            );
            assertThat(MatchMetrics.precisionAtK(results, Set.of(), 5)).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Edge case: empty/zero → 0.0")
        void precisionAtK_edgeCases() {
            assertThat(MatchMetrics.precisionAtK(List.of(), Set.of(), 0)).isEqualTo(0.0);
        }
    }

    // ====================== NDCG@K ======================

    @Nested
    @DisplayName("NDCG@K")
    class NdcgTests {

        @Test
        @DisplayName("Perfect ranking → 1.0")
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
        @DisplayName("Swapped ranking < 1.0")
        void ndcgAtK_swapped() {
            UUID u1 = UUID.randomUUID(), u2 = UUID.randomUUID();
            List<MatchResponse> results = List.of(
                    MatchResponse.builder().userId(u2).build(), // lower relevance first
                    MatchResponse.builder().userId(u1).build()
            );
            Map<UUID, Double> rel = Map.of(u1, 3.0, u2, 1.0);
            double ndcg = MatchMetrics.ndcgAtK(results, rel, 2);
            assertThat(ndcg).isLessThan(1.0).isGreaterThan(0.0);
        }

        @Test
        @DisplayName("Edge case: empty → 0.0")
        void ndcgAtK_empty() {
            assertThat(MatchMetrics.ndcgAtK(List.of(), Map.of(), 5)).isEqualTo(0.0);
        }
    }

    // ====================== MAP@K ======================

    @Nested
    @DisplayName("MAP@K")
    class MapTests {

        @Test
        @DisplayName("Single query, perfect precision → AP=1.0")
        void averagePrecision_perfect() {
            UUID u1 = UUID.randomUUID(), u2 = UUID.randomUUID();
            List<MatchResponse> results = List.of(
                    MatchResponse.builder().userId(u1).build(),
                    MatchResponse.builder().userId(u2).build()
            );
            double ap = MatchMetrics.averagePrecisionAtK(results, Set.of(u1, u2), 2);
            assertThat(ap).isCloseTo(1.0, within(0.001));
        }

        @Test
        @DisplayName("Relevant at position 2 only → AP < 1.0")
        void averagePrecision_lateRelevance() {
            UUID u1 = UUID.randomUUID(), u2 = UUID.randomUUID();
            List<MatchResponse> results = List.of(
                    MatchResponse.builder().userId(u1).build(), // not relevant
                    MatchResponse.builder().userId(u2).build()  // relevant
            );
            double ap = MatchMetrics.averagePrecisionAtK(results, Set.of(u2), 2);
            assertThat(ap).isCloseTo(0.5, within(0.001)); // precision at rank 2 = 0.5
        }

        @Test
        @DisplayName("MAP across multiple queries")
        void mapAcrossQueries() {
            UUID u1 = UUID.randomUUID(), u2 = UUID.randomUUID(), u3 = UUID.randomUUID();

            List<List<MatchResponse>> allResults = List.of(
                    List.of(MatchResponse.builder().userId(u1).build(),
                            MatchResponse.builder().userId(u2).build()),
                    List.of(MatchResponse.builder().userId(u3).build())
            );
            List<Set<UUID>> allRelevant = List.of(
                    Set.of(u1, u2), // both relevant → AP=1.0
                    Set.of(u3)      // relevant → AP=1.0
            );

            double map = MatchMetrics.mapAtK(allResults, allRelevant, 5);
            assertThat(map).isCloseTo(1.0, within(0.001));
        }

        @Test
        @DisplayName("No relevant results → AP=0.0")
        void averagePrecision_noRelevant() {
            assertThat(MatchMetrics.averagePrecisionAtK(List.of(), Set.of(), 5)).isEqualTo(0.0);
        }
    }

    // ====================== MRR ======================

    @Nested
    @DisplayName("MRR")
    class MrrTests {

        @Test
        @DisplayName("Relevant at rank 1 → RR=1.0")
        void rr_rank1() {
            UUID u1 = UUID.randomUUID();
            List<MatchResponse> results = List.of(
                    MatchResponse.builder().userId(u1).build()
            );
            assertThat(MatchMetrics.reciprocalRank(results, Set.of(u1))).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Relevant at rank 3 → RR=1/3")
        void rr_rank3() {
            UUID u1 = UUID.randomUUID(), u2 = UUID.randomUUID(), u3 = UUID.randomUUID();
            List<MatchResponse> results = List.of(
                    MatchResponse.builder().userId(u1).build(),
                    MatchResponse.builder().userId(u2).build(),
                    MatchResponse.builder().userId(u3).build()
            );
            assertThat(MatchMetrics.reciprocalRank(results, Set.of(u3)))
                    .isCloseTo(1.0 / 3, within(0.001));
        }

        @Test
        @DisplayName("No relevant → RR=0.0")
        void rr_noRelevant() {
            UUID u1 = UUID.randomUUID();
            List<MatchResponse> results = List.of(
                    MatchResponse.builder().userId(u1).build()
            );
            assertThat(MatchMetrics.reciprocalRank(results, Set.of())).isEqualTo(0.0);
        }

        @Test
        @DisplayName("MRR across queries")
        void mrr_multiple() {
            UUID u1 = UUID.randomUUID(), u2 = UUID.randomUUID();
            List<List<MatchResponse>> allResults = List.of(
                    List.of(MatchResponse.builder().userId(u1).build()),
                    List.of(MatchResponse.builder().userId(u2).build())
            );
            List<Set<UUID>> allRelevant = List.of(Set.of(u1), Set.of(u2));
            assertThat(MatchMetrics.mrr(allResults, allRelevant)).isEqualTo(1.0);
        }
    }

    // ====================== Coverage ======================

    @Nested
    @DisplayName("Coverage")
    class CoverageTests {

        @Test
        @DisplayName("All queries have relevant results → 1.0")
        void coverage_full() {
            UUID u1 = UUID.randomUUID(), u2 = UUID.randomUUID();
            List<List<MatchResponse>> allResults = List.of(
                    List.of(MatchResponse.builder().userId(u1).build()),
                    List.of(MatchResponse.builder().userId(u2).build())
            );
            List<Set<UUID>> allRelevant = List.of(Set.of(u1), Set.of(u2));
            assertThat(MatchMetrics.coverage(allResults, allRelevant, 5)).isEqualTo(1.0);
        }

        @Test
        @DisplayName("No queries have relevant results → 0.0")
        void coverage_none() {
            UUID u1 = UUID.randomUUID();
            List<List<MatchResponse>> allResults = List.of(
                    List.of(MatchResponse.builder().userId(u1).build())
            );
            List<Set<UUID>> allRelevant = List.of(Set.of()); // nothing relevant
            assertThat(MatchMetrics.coverage(allResults, allRelevant, 5)).isEqualTo(0.0);
        }
    }

    // ====================== Diversity ======================

    @Nested
    @DisplayName("Diversity")
    class DiversityTests {

        @Test
        @DisplayName("All same goal type → low diversity")
        void goalDiversity_homogeneous() {
            List<MatchResponse> results = List.of(
                    MatchResponse.builder().userId(UUID.randomUUID()).goalType("BUILDING").build(),
                    MatchResponse.builder().userId(UUID.randomUUID()).goalType("BUILDING").build(),
                    MatchResponse.builder().userId(UUID.randomUUID()).goalType("BUILDING").build()
            );
            assertThat(MatchMetrics.goalTypeDiversity(results, 3)).isLessThanOrEqualTo(0.34);
        }

        @Test
        @DisplayName("Mixed goal types → higher diversity")
        void goalDiversity_mixed() {
            List<MatchResponse> results = List.of(
                    MatchResponse.builder().userId(UUID.randomUUID()).goalType("BUILDING").build(),
                    MatchResponse.builder().userId(UUID.randomUUID()).goalType("LEARNING").build(),
                    MatchResponse.builder().userId(UUID.randomUUID()).goalType("MENTORING").build(),
                    MatchResponse.builder().userId(UUID.randomUUID()).goalType("EXPLORING").build()
            );
            assertThat(MatchMetrics.goalTypeDiversity(results, 4)).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Experience diversity with mixed levels")
        void experienceDiversity_mixed() {
            List<MatchResponse> results = List.of(
                    MatchResponse.builder().userId(UUID.randomUUID()).experienceLevel("BEGINNER").build(),
                    MatchResponse.builder().userId(UUID.randomUUID()).experienceLevel("ADVANCED").build(),
                    MatchResponse.builder().userId(UUID.randomUUID()).experienceLevel("EXPERT").build()
            );
            assertThat(MatchMetrics.experienceDiversity(results, 3)).isGreaterThan(0.5);
        }

        @Test
        @DisplayName("Empty results → 0.0")
        void diversity_empty() {
            assertThat(MatchMetrics.goalTypeDiversity(List.of(), 5)).isEqualTo(0.0);
            assertThat(MatchMetrics.experienceDiversity(List.of(), 5)).isEqualTo(0.0);
        }
    }
}
