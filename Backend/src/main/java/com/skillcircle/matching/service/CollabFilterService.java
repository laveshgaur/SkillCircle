package com.skillcircle.matching.service;

import com.skillcircle.matching.dto.ScoredCandidate;
import com.skillcircle.matching.entity.CollaborationEdge;
import com.skillcircle.matching.repository.CollaborationEdgeRepository;
import com.skillcircle.profile.entity.GoalType;
import com.skillcircle.profile.entity.Profile;
import com.skillcircle.profile.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Stage 2: Collaborative Filtering Re-Ranker.
 *
 * Takes the top-K semantic candidates from Stage 1 and re-ranks them
 * using a weighted combination of:
 *   - α × semantic similarity (from vector DB)
 *   - β × collaboration score (from co-contribution graph)
 *   - γ × goal alignment score
 *
 * Cold-start handling: β → 0 for users with no collaboration history.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CollabFilterService {

    private final CollaborationEdgeRepository collabEdgeRepository;
    private final ProfileRepository profileRepository;

    /**
     * Re-rank candidates using the weighted scoring formula.
     *
     * @param userId             the requesting user's ID
     * @param semanticCandidates candidates from Stage 1 with semantic scores
     * @param alpha              weight for semantic similarity
     * @param beta               weight for collaboration score
     * @param gamma              weight for goal alignment
     * @return re-ranked list of ScoredCandidates
     */
    public List<ScoredCandidate> reRank(
            UUID userId,
            List<ScoredCandidate> semanticCandidates,
            double alpha, double beta, double gamma) {

        if (semanticCandidates.isEmpty()) {
            return Collections.emptyList();
        }

        // Load user's profile for goal alignment
        Profile userProfile = profileRepository.findByUserId(userId).orElse(null);
        GoalType userGoalType = userProfile != null ? userProfile.getGoalType() : null;

        // Load all collaboration edges for this user
        List<CollaborationEdge> edges = collabEdgeRepository.findByUserId(userId);
        Map<UUID, Double> collabScores = buildCollabScoreMap(userId, edges);

        // Cold-start: if no collaboration history, redistribute β to α
        boolean isColdStart = collabScores.isEmpty();
        double effectiveAlpha = isColdStart ? alpha + beta : alpha;
        double effectiveBeta = isColdStart ? 0.0 : beta;

        // Re-rank each candidate
        for (ScoredCandidate candidate : semanticCandidates) {
            double collabScore = collabScores.getOrDefault(candidate.getUserId(), 0.0);
            double goalScore = computeGoalAlignment(userGoalType, candidate.getUserId());

            candidate.setCollabScore(collabScore);
            candidate.setGoalScore(goalScore);

            double finalScore = effectiveAlpha * candidate.getSemanticScore()
                    + effectiveBeta * collabScore
                    + gamma * goalScore;
            candidate.setFinalScore(finalScore);
        }

        // Sort by final score descending
        semanticCandidates.sort(Comparator.comparingDouble(ScoredCandidate::getFinalScore).reversed());

        log.debug("Re-ranked {} candidates (coldStart={}, α={}, β={}, γ={})",
                semanticCandidates.size(), isColdStart, effectiveAlpha, effectiveBeta, gamma);

        return semanticCandidates;
    }

    /**
     * Build a map of userId → normalized collaboration score from edges.
     */
    private Map<UUID, Double> buildCollabScoreMap(UUID userId, List<CollaborationEdge> edges) {
        Map<UUID, Double> scoreMap = new HashMap<>();

        for (CollaborationEdge edge : edges) {
            UUID partnerId = edge.getUserAId().equals(userId) ? edge.getUserBId() : edge.getUserAId();
            scoreMap.merge(partnerId, (double) edge.getStrength(), Double::sum);
        }

        // Normalize to [0, 1] range
        if (!scoreMap.isEmpty()) {
            double maxScore = Collections.max(scoreMap.values());
            if (maxScore > 0) {
                scoreMap.replaceAll((k, v) -> v / maxScore);
            }
        }

        return scoreMap;
    }

    /**
     * Compute goal alignment score between two users.
     *
     * Goal alignment matrix:
     * - Same goal type → 1.0 (both learning, both building)
     * - Complementary → 0.8 (learning ↔ mentoring)
     * - Partial → 0.5 (building ↔ exploring)
     * - No match → 0.2
     */
    private double computeGoalAlignment(GoalType userGoal, UUID candidateUserId) {
        if (userGoal == null) return 0.5; // neutral

        Profile candidateProfile = profileRepository.findByUserId(candidateUserId).orElse(null);
        if (candidateProfile == null || candidateProfile.getGoalType() == null) return 0.5;

        GoalType candidateGoal = candidateProfile.getGoalType();

        if (userGoal == candidateGoal) return 1.0;

        // Complementary pairs
        if ((userGoal == GoalType.LEARNING && candidateGoal == GoalType.MENTORING) ||
            (userGoal == GoalType.MENTORING && candidateGoal == GoalType.LEARNING)) {
            return 0.8;
        }

        // Partial alignment
        if ((userGoal == GoalType.BUILDING && candidateGoal == GoalType.EXPLORING) ||
            (userGoal == GoalType.EXPLORING && candidateGoal == GoalType.BUILDING)) {
            return 0.5;
        }

        return 0.2; // No meaningful alignment
    }
}
