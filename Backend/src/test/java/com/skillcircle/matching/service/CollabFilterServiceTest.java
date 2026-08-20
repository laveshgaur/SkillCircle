package com.skillcircle.matching.service;

import com.skillcircle.auth.entity.User;
import com.skillcircle.matching.dto.ScoredCandidate;
import com.skillcircle.matching.entity.CollaborationEdge;
import com.skillcircle.matching.repository.CollaborationEdgeRepository;
import com.skillcircle.profile.entity.Availability;
import com.skillcircle.profile.entity.GoalType;
import com.skillcircle.profile.entity.Profile;
import com.skillcircle.profile.repository.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollabFilterServiceTest {

    @Mock private CollaborationEdgeRepository collabEdgeRepository;
    @Mock private ProfileRepository profileRepository;

    @InjectMocks private CollabFilterService collabFilterService;

    private UUID userId;
    private UUID candidateA;
    private UUID candidateB;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        candidateA = UUID.randomUUID();
        candidateB = UUID.randomUUID();
    }

    @Test
    @DisplayName("Cold-start: no collab edges should redistribute beta to alpha")
    void reRank_coldStart_shouldRedistributeBeta() {
        List<ScoredCandidate> candidates = List.of(
                ScoredCandidate.builder().userId(candidateA).semanticScore(0.9).build(),
                ScoredCandidate.builder().userId(candidateB).semanticScore(0.7).build()
        );
        candidates = new ArrayList<>(candidates);

        // No collab edges → cold start
        when(collabEdgeRepository.findByUserId(userId)).thenReturn(Collections.emptyList());
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        List<ScoredCandidate> result = collabFilterService.reRank(
                userId, candidates, 0.5, 0.3, 0.2);

        // With cold start: effectiveAlpha = 0.8, effectiveBeta = 0
        // candidateA: 0.8 * 0.9 + 0 + 0.2 * 0.5 = 0.82
        // candidateB: 0.8 * 0.7 + 0 + 0.2 * 0.5 = 0.66
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getUserId()).isEqualTo(candidateA);
        assertThat(result.get(0).getFinalScore()).isGreaterThan(result.get(1).getFinalScore());
    }

    @Test
    @DisplayName("With collab edges, collaboration score should boost ranking")
    void reRank_withCollabEdges_shouldBoost() {
        List<ScoredCandidate> candidates = new ArrayList<>(List.of(
                ScoredCandidate.builder().userId(candidateA).semanticScore(0.7).build(),
                ScoredCandidate.builder().userId(candidateB).semanticScore(0.9).build()
        ));

        // candidateA has strong collab with user, candidateB has none
        CollaborationEdge edge = CollaborationEdge.builder()
                .userAId(userId).userBId(candidateA)
                .source("github_repo").strength(5.0f).build();
        when(collabEdgeRepository.findByUserId(userId)).thenReturn(List.of(edge));
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        List<ScoredCandidate> result = collabFilterService.reRank(
                userId, candidates, 0.5, 0.3, 0.2);

        // candidateA should get collab boost
        assertThat(result.get(0).getCollabScore()).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("Same goal type should give maximum goal alignment")
    void reRank_sameGoalType_shouldGiveMaxAlignment() {
        User user = User.builder().id(userId).build();
        Profile userProfile = Profile.builder().user(user)
                .goalType(GoalType.BUILDING).availability(Availability.OPEN)
                .profileSkills(new ArrayList<>()).build();
        Profile candidateProfile = Profile.builder()
                .user(User.builder().id(candidateA).build())
                .goalType(GoalType.BUILDING).availability(Availability.OPEN)
                .profileSkills(new ArrayList<>()).build();

        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(userProfile));
        when(profileRepository.findByUserId(candidateA)).thenReturn(Optional.of(candidateProfile));
        when(collabEdgeRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

        List<ScoredCandidate> candidates = new ArrayList<>(List.of(
                ScoredCandidate.builder().userId(candidateA).semanticScore(0.8).build()
        ));

        List<ScoredCandidate> result = collabFilterService.reRank(
                userId, candidates, 0.5, 0.3, 0.2);

        assertThat(result.get(0).getGoalScore()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Empty candidate list should return empty")
    void reRank_emptyList_shouldReturnEmpty() {
        List<ScoredCandidate> result = collabFilterService.reRank(
                userId, Collections.emptyList(), 0.5, 0.3, 0.2);
        assertThat(result).isEmpty();
    }
}
