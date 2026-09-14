package com.skillcircle.matching.evaluation;

import com.skillcircle.matching.dto.BenchmarkResult;
import com.skillcircle.matching.dto.EvaluationReport;
import com.skillcircle.matching.evaluation.SyntheticDataGenerator.*;
import com.skillcircle.profile.entity.GoalType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class EvaluationServiceTest {

    private EvaluationService evaluationService;

    @BeforeEach
    void setUp() {
        evaluationService = new EvaluationService();
    }

    // ====================== Synthetic Data Generator ======================

    @Nested
    @DisplayName("SyntheticDataGenerator")
    class GeneratorTests {

        @Test
        @DisplayName("Generates the correct number of users")
        void generatesCorrectCount() {
            SyntheticDataset dataset = SyntheticDataGenerator.generate(50, 42L);
            assertThat(dataset.getUsers()).hasSize(50);
        }

        @Test
        @DisplayName("Each user has valid skills")
        void usersHaveSkills() {
            SyntheticDataset dataset = SyntheticDataGenerator.generate(20, 42L);
            for (SyntheticUser user : dataset.getUsers()) {
                assertThat(user.getSkills()).isNotEmpty();
                assertThat(user.getSkills().size()).isBetween(4, 9);
                assertThat(user.getGoalType()).isNotNull();
                assertThat(user.getTimezone()).isNotNull();
                assertThat(user.getExperienceLevel()).isNotNull();
            }
        }

        @Test
        @DisplayName("Every 5th user is cold-start")
        void coldStartUsersExist() {
            SyntheticDataset dataset = SyntheticDataGenerator.generate(50, 42L);
            assertThat(dataset.getColdStartUsers()).hasSize(10); // 50/5
            assertThat(dataset.getWarmStartUsers()).hasSize(40);
        }

        @Test
        @DisplayName("Collaboration edges only between warm-start users")
        void collabEdgesValid() {
            SyntheticDataset dataset = SyntheticDataGenerator.generate(50, 42L);
            Set<UUID> coldIds = new HashSet<>();
            dataset.getColdStartUsers().forEach(u -> coldIds.add(u.getId()));

            for (CollabEdge edge : dataset.getCollabEdges()) {
                assertThat(coldIds).doesNotContain(edge.getUserAId());
                assertThat(coldIds).doesNotContain(edge.getUserBId());
                assertThat(edge.getStrength()).isPositive();
            }
        }

        @Test
        @DisplayName("Ground truth covers all user pairs")
        void groundTruthComplete() {
            SyntheticDataset dataset = SyntheticDataGenerator.generate(20, 42L);
            assertThat(dataset.getGroundTruth()).hasSize(20);
            for (Map.Entry<UUID, Map<UUID, Double>> entry : dataset.getGroundTruth().entrySet()) {
                // Each user should have scores for all other users
                assertThat(entry.getValue()).hasSize(19); // 20 - self
                for (Double score : entry.getValue().values()) {
                    assertThat(score).isBetween(0.0, 3.0);
                }
            }
        }

        @Test
        @DisplayName("Deterministic with same seed")
        void deterministic() {
            SyntheticDataset d1 = SyntheticDataGenerator.generate(30, 123L);
            SyntheticDataset d2 = SyntheticDataGenerator.generate(30, 123L);

            for (int i = 0; i < 30; i++) {
                assertThat(d1.getUsers().get(i).getId()).isEqualTo(d2.getUsers().get(i).getId());
                assertThat(d1.getUsers().get(i).getSkills()).isEqualTo(d2.getUsers().get(i).getSkills());
            }
        }
    }

    // ====================== Similarity Functions ======================

    @Nested
    @DisplayName("Similarity Functions")
    class SimilarityTests {

        @Test
        @DisplayName("Jaccard similarity: identical sets → 1.0")
        void jaccard_identical() {
            Set<String> a = Set.of("Java", "Spring", "React");
            assertThat(SyntheticDataGenerator.jaccardSimilarity(a, a)).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Jaccard similarity: disjoint sets → 0.0")
        void jaccard_disjoint() {
            Set<String> a = Set.of("Java", "Spring");
            Set<String> b = Set.of("React", "Vue");
            assertThat(SyntheticDataGenerator.jaccardSimilarity(a, b)).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Jaccard similarity: partial overlap")
        void jaccard_partial() {
            Set<String> a = Set.of("Java", "Spring", "React");
            Set<String> b = Set.of("Java", "React", "Vue");
            // Intersection: {Java, React} = 2, Union: {Java, Spring, React, Vue} = 4
            assertThat(SyntheticDataGenerator.jaccardSimilarity(a, b)).isEqualTo(0.5);
        }

        @Test
        @DisplayName("Goal alignment: same → 1.0, complementary → 0.8")
        void goalAlignment() {
            assertThat(SyntheticDataGenerator.goalAlignmentScore(GoalType.LEARNING, GoalType.LEARNING)).isEqualTo(1.0);
            assertThat(SyntheticDataGenerator.goalAlignmentScore(GoalType.LEARNING, GoalType.MENTORING)).isEqualTo(0.8);
            assertThat(SyntheticDataGenerator.goalAlignmentScore(GoalType.BUILDING, GoalType.EXPLORING)).isEqualTo(0.5);
            assertThat(SyntheticDataGenerator.goalAlignmentScore(GoalType.LEARNING, GoalType.BUILDING)).isEqualTo(0.2);
        }

        @Test
        @DisplayName("Timezone proximity: same → 1.0, distant → low")
        void timezoneProximity() {
            assertThat(SyntheticDataGenerator.timezoneProximity("UTC+0", "UTC+0")).isEqualTo(1.0);
            assertThat(SyntheticDataGenerator.timezoneProximity("UTC+0", "UTC+1")).isGreaterThan(0.9);
            assertThat(SyntheticDataGenerator.timezoneProximity("UTC-8", "UTC+8")).isLessThan(0.5);
        }
    }

    // ====================== Evaluation Report ======================

    @Nested
    @DisplayName("Evaluation Report")
    class ReportTests {

        @Test
        @DisplayName("Report contains 6 benchmark configurations")
        void reportHasBenchmarks() {
            EvaluationReport report = evaluationService.runEvaluation(30, 42L);

            assertThat(report).isNotNull();
            assertThat(report.getBenchmarks()).hasSize(6);
            assertThat(report.getGeneratedAt()).isNotNull();
            assertThat(report.getTotalSyntheticUsers()).isEqualTo(30);
            assertThat(report.getBestConfiguration()).isNotBlank();
            assertThat(report.getRecommendedWeights()).isNotNull();
        }

        @Test
        @DisplayName("All benchmarks have valid metric values")
        void metricsInRange() {
            EvaluationReport report = evaluationService.runEvaluation(30, 42L);

            for (BenchmarkResult bench : report.getBenchmarks()) {
                assertThat(bench.getConfigName()).isNotBlank();
                assertThat(bench.getPrecisionAt5()).isBetween(0.0, 1.0);
                assertThat(bench.getPrecisionAt10()).isBetween(0.0, 1.0);
                assertThat(bench.getNdcgAt5()).isBetween(0.0, 1.0);
                assertThat(bench.getNdcgAt10()).isBetween(0.0, 1.0);
                assertThat(bench.getMapAt10()).isBetween(0.0, 1.0);
                assertThat(bench.getMrr()).isBetween(0.0, 1.0);
                assertThat(bench.getCoverage()).isBetween(0.0, 1.0);
                assertThat(bench.getGoalTypeDiversity()).isBetween(0.0, 1.0);
                assertThat(bench.getExperienceDiversity()).isBetween(0.0, 1.0);
            }
        }

        @Test
        @DisplayName("Semantic-only baseline exists and is config 1")
        void semanticOnlyBaseline() {
            EvaluationReport report = evaluationService.runEvaluation(30, 42L);

            BenchmarkResult baseline = report.getBenchmarks().get(0);
            assertThat(baseline.getConfigName()).contains("Semantic-Only");
            assertThat(baseline.getWeights().getBeta()).isEqualTo(0.0);
            assertThat(baseline.getWeights().getGamma()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Hybrid default uses production weights")
        void hybridConfig() {
            EvaluationReport report = evaluationService.runEvaluation(30, 42L);

            BenchmarkResult hybrid = report.getBenchmarks().get(1);
            assertThat(hybrid.getConfigName()).contains("Hybrid");
            assertThat(hybrid.getWeights().getAlpha()).isEqualTo(0.5);
            assertThat(hybrid.getWeights().getBeta()).isEqualTo(0.3);
            assertThat(hybrid.getWeights().getGamma()).isEqualTo(0.2);
        }

        @Test
        @DisplayName("Cold-start cohort has no collab edges")
        void coldStartCohort() {
            EvaluationReport report = evaluationService.runEvaluation(50, 42L);

            BenchmarkResult coldStart = report.getBenchmarks().stream()
                    .filter(b -> b.getConfigName().contains("Cold-Start"))
                    .findFirst()
                    .orElseThrow();

            assertThat(coldStart.isHasCollabEdges()).isFalse();
            assertThat(coldStart.getQueriesEvaluated()).isPositive();
        }

        @Test
        @DisplayName("Coverage is positive for all configurations")
        void coveragePositive() {
            EvaluationReport report = evaluationService.runEvaluation(30, 42L);

            for (BenchmarkResult bench : report.getBenchmarks()) {
                assertThat(bench.getCoverage())
                        .as("Coverage for " + bench.getConfigName())
                        .isGreaterThan(0.0);
            }
        }

        @Test
        @DisplayName("Deterministic evaluation produces same results")
        void deterministic() {
            EvaluationReport r1 = evaluationService.runEvaluation(30, 42L);
            EvaluationReport r2 = evaluationService.runEvaluation(30, 42L);

            for (int i = 0; i < r1.getBenchmarks().size(); i++) {
                assertThat(r1.getBenchmarks().get(i).getNdcgAt10())
                        .isEqualTo(r2.getBenchmarks().get(i).getNdcgAt10());
                assertThat(r1.getBenchmarks().get(i).getPrecisionAt5())
                        .isEqualTo(r2.getBenchmarks().get(i).getPrecisionAt5());
            }
        }
    }
}
