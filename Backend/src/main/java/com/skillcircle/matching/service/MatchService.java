package com.skillcircle.matching.service;

import com.skillcircle.auth.entity.User;
import com.skillcircle.exception.BadRequestException;
import com.skillcircle.exception.ResourceNotFoundException;
import com.skillcircle.matching.dto.MatchRequest;
import com.skillcircle.matching.dto.MatchResponse;
import com.skillcircle.matching.dto.ScoredCandidate;
import com.skillcircle.matching.entity.MatchRecord;
import com.skillcircle.matching.repository.MatchRecordRepository;
import com.skillcircle.profile.entity.Profile;
import com.skillcircle.profile.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Core matching engine orchestrating the 3-stage pipeline:
 *
 *   Stage 1: Semantic Retrieval (EmbeddingService + VectorDBClient)
 *            → top-K ANN candidates by cosine similarity
 *
 *   Stage 2: Collaborative Filtering Re-Rank (CollabFilterService)
 *            → weighted combination: α×semantic + β×collab + γ×goal
 *
 *   Stage 3: Hard Filters (HardFilterService)
 *            → timezone overlap, availability, goal type compatibility
 *
 * Results are persisted as MatchRecords for history/accept/dismiss.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchService {

    private final EmbeddingService embeddingService;
    private final VectorDBClient vectorDBClient;
    private final CollabFilterService collabFilterService;
    private final HardFilterService hardFilterService;
    private final MatchRecordRepository matchRecordRepository;
    private final ProfileRepository profileRepository;

    @Value("${app.matching.weight.semantic:0.5}")
    private double alphaWeight;

    @Value("${app.matching.weight.collab:0.3}")
    private double betaWeight;

    @Value("${app.matching.weight.goal:0.2}")
    private double gammaWeight;

    @Value("${app.matching.top-k:50}")
    private int topK;

    /**
     * Execute the full 3-stage matching pipeline.
     */
    @Transactional
    public List<MatchResponse> findMatches(User user, MatchRequest request) {
        UUID userId = user.getId();
        int limit = request.getLimit() != null ? Math.min(request.getLimit(), 20) : 10;

        // Ensure user has a profile
        Profile profile = profileRepository.findByUserIdWithSkills(userId)
                .orElseThrow(() -> new BadRequestException(
                        "Create a profile before searching for matches"));

        // ============ Stage 1: Semantic Retrieval ============
        float[] userVector = embeddingService.getOrComputeVector(userId);

        List<VectorDBClient.VectorMatch> annResults =
                vectorDBClient.searchANN(userVector, topK, userId.toString());

        // Convert to ScoredCandidates
        List<ScoredCandidate> candidates = annResults.stream()
                .map(vm -> ScoredCandidate.builder()
                        .userId(parseUUID(vm.id()))
                        .semanticScore(vm.score())
                        .build())
                .filter(c -> c.getUserId() != null)
                .collect(Collectors.toCollection(ArrayList::new));

        log.info("Stage 1 (Semantic): {} candidates for user {}", candidates.size(), userId);

        // ============ Stage 2: Collaborative Filtering Re-Rank ============
        candidates = collabFilterService.reRank(userId, candidates, alphaWeight, betaWeight, gammaWeight);

        log.info("Stage 2 (CollabFilter): {} candidates re-ranked", candidates.size());

        // ============ Stage 3: Hard Filters ============
        candidates = hardFilterService.apply(
                candidates,
                request.getGoalType(),
                request.getAvailability(),
                request.getTimezoneOverlapHours(),
                userId
        );

        log.info("Stage 3 (HardFilter): {} candidates after filtering", candidates.size());

        // ============ Build Response & Persist ============
        List<ScoredCandidate> topResults = candidates.stream()
                .limit(limit)
                .toList();

        List<MatchResponse> responses = new ArrayList<>();
        for (ScoredCandidate candidate : topResults) {
            // Persist match record
            MatchRecord record = MatchRecord.builder()
                    .requesterId(userId)
                    .matchedUserId(candidate.getUserId())
                    .semanticScore(candidate.getSemanticScore())
                    .collabScore(candidate.getCollabScore())
                    .goalScore(candidate.getGoalScore())
                    .finalScore(candidate.getFinalScore())
                    .status(MatchRecord.MatchStatus.PENDING)
                    .build();
            record = matchRecordRepository.save(record);

            // Build response with profile data
            MatchResponse response = buildMatchResponse(record, candidate);
            responses.add(response);
        }

        log.info("Match pipeline complete for user {}: {} results returned", userId, responses.size());
        return responses;
    }

    /**
     * Get match history for the authenticated user.
     */
    @Transactional(readOnly = true)
    public List<MatchResponse> getMatchHistory(UUID userId) {
        List<MatchRecord> records = matchRecordRepository
                .findByRequesterIdOrderByCreatedAtDesc(userId);

        return records.stream()
                .map(r -> buildMatchResponseFromRecord(r))
                .toList();
    }

    /**
     * Accept a match.
     */
    @Transactional
    public MatchResponse acceptMatch(UUID userId, UUID matchId) {
        MatchRecord record = matchRecordRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match", "id", matchId));

        if (!record.getRequesterId().equals(userId)) {
            throw new BadRequestException("You can only accept your own matches");
        }

        record.setStatus(MatchRecord.MatchStatus.ACCEPTED);
        record = matchRecordRepository.save(record);

        log.info("Match {} accepted by user {}", matchId, userId);
        return buildMatchResponseFromRecord(record);
    }

    /**
     * Dismiss a match.
     */
    @Transactional
    public MatchResponse dismissMatch(UUID userId, UUID matchId) {
        MatchRecord record = matchRecordRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match", "id", matchId));

        if (!record.getRequesterId().equals(userId)) {
            throw new BadRequestException("You can only dismiss your own matches");
        }

        record.setStatus(MatchRecord.MatchStatus.DISMISSED);
        record = matchRecordRepository.save(record);

        log.info("Match {} dismissed by user {}", matchId, userId);
        return buildMatchResponseFromRecord(record);
    }

    // ===================== Internal Helpers =====================

    private MatchResponse buildMatchResponse(MatchRecord record, ScoredCandidate candidate) {
        Profile matchedProfile = profileRepository
                .findByUserIdWithSkills(candidate.getUserId()).orElse(null);

        MatchResponse.MatchResponseBuilder builder = MatchResponse.builder()
                .matchId(record.getId())
                .userId(candidate.getUserId())
                .semanticScore(candidate.getSemanticScore())
                .collabScore(candidate.getCollabScore())
                .goalScore(candidate.getGoalScore())
                .finalScore(candidate.getFinalScore())
                .status(record.getStatus().name())
                .matchedAt(record.getCreatedAt());

        if (matchedProfile != null) {
            builder.username(matchedProfile.getUser().getUsername())
                    .displayName(matchedProfile.getDisplayName())
                    .avatarUrl(matchedProfile.getUser().getAvatarUrl())
                    .bio(matchedProfile.getBio())
                    .goalType(matchedProfile.getGoalType() != null
                            ? matchedProfile.getGoalType().name() : null)
                    .experienceLevel(matchedProfile.getExperienceLevel() != null
                            ? matchedProfile.getExperienceLevel().name() : null)
                    .timezone(matchedProfile.getTimezone())
                    .skills(matchedProfile.getProfileSkills().stream()
                            .map(ps -> ps.getSkill().getName())
                            .toList());
        }

        return builder.build();
    }

    private MatchResponse buildMatchResponseFromRecord(MatchRecord record) {
        Profile matchedProfile = profileRepository
                .findByUserIdWithSkills(record.getMatchedUserId()).orElse(null);

        MatchResponse.MatchResponseBuilder builder = MatchResponse.builder()
                .matchId(record.getId())
                .userId(record.getMatchedUserId())
                .semanticScore(record.getSemanticScore())
                .collabScore(record.getCollabScore())
                .goalScore(record.getGoalScore())
                .finalScore(record.getFinalScore())
                .status(record.getStatus().name())
                .matchedAt(record.getCreatedAt());

        if (matchedProfile != null) {
            builder.username(matchedProfile.getUser().getUsername())
                    .displayName(matchedProfile.getDisplayName())
                    .avatarUrl(matchedProfile.getUser().getAvatarUrl())
                    .bio(matchedProfile.getBio())
                    .goalType(matchedProfile.getGoalType() != null
                            ? matchedProfile.getGoalType().name() : null)
                    .experienceLevel(matchedProfile.getExperienceLevel() != null
                            ? matchedProfile.getExperienceLevel().name() : null)
                    .timezone(matchedProfile.getTimezone())
                    .skills(matchedProfile.getProfileSkills().stream()
                            .map(ps -> ps.getSkill().getName())
                            .toList());
        }

        return builder.build();
    }

    private UUID parseUUID(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid UUID from vector DB: {}", id);
            return null;
        }
    }
}
