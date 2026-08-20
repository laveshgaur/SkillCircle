package com.skillcircle.matching.service;

import com.skillcircle.auth.entity.User;
import com.skillcircle.matching.dto.ScoredCandidate;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HardFilterServiceTest {

    @Mock private ProfileRepository profileRepository;

    @InjectMocks private HardFilterService hardFilterService;

    private UUID requesterId;
    private UUID openUserId;
    private UUID busyUserId;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(hardFilterService, "defaultTimezoneOverlapHours", 4);

        requesterId = UUID.randomUUID();
        openUserId = UUID.randomUUID();
        busyUserId = UUID.randomUUID();

        // Requester profile
        Profile requesterProfile = Profile.builder()
                .user(User.builder().id(requesterId).build())
                .availability(Availability.OPEN)
                .timezone("Asia/Kolkata") // UTC+5:30
                .profileSkills(new ArrayList<>())
                .build();
        lenient().when(profileRepository.findByUserId(requesterId)).thenReturn(Optional.of(requesterProfile));

        // Open user profile (same timezone)
        Profile openProfile = Profile.builder()
                .user(User.builder().id(openUserId).build())
                .availability(Availability.OPEN)
                .goalType(GoalType.BUILDING)
                .timezone("Asia/Kolkata")
                .profileSkills(new ArrayList<>())
                .build();
        lenient().when(profileRepository.findByUserId(openUserId)).thenReturn(Optional.of(openProfile));

        // Busy user profile
        Profile busyProfile = Profile.builder()
                .user(User.builder().id(busyUserId).build())
                .availability(Availability.BUSY)
                .goalType(GoalType.LEARNING)
                .timezone("America/New_York") // UTC-5
                .profileSkills(new ArrayList<>())
                .build();
        lenient().when(profileRepository.findByUserId(busyUserId)).thenReturn(Optional.of(busyProfile));
    }

    @Test
    @DisplayName("Should filter out BUSY users when requiring OPEN availability")
    void apply_shouldFilterBusyUsers() {
        List<ScoredCandidate> candidates = List.of(
                ScoredCandidate.builder().userId(openUserId).finalScore(0.9).build(),
                ScoredCandidate.builder().userId(busyUserId).finalScore(0.8).build()
        );

        List<ScoredCandidate> result = hardFilterService.apply(
                candidates, null, "OPEN", null, requesterId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(openUserId);
    }

    @Test
    @DisplayName("Should filter by goal type")
    void apply_shouldFilterByGoalType() {
        List<ScoredCandidate> candidates = List.of(
                ScoredCandidate.builder().userId(openUserId).finalScore(0.9).build(),
                ScoredCandidate.builder().userId(busyUserId).finalScore(0.8).build()
        );

        // busy user has LEARNING goal, open user has BUILDING
        List<ScoredCandidate> result = hardFilterService.apply(
                candidates, "BUILDING", null, null, requesterId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(openUserId);
    }

    @Test
    @DisplayName("Timezone overlap calculation should be correct")
    void calculateOverlapHours_shouldBeCorrect() {
        // Same timezone → 12 hours overlap
        assertThat(hardFilterService.calculateOverlapHours(
                "Asia/Kolkata", "Asia/Kolkata")).isEqualTo(12);

        // UTC+5:30 vs UTC-5 = 10.5h diff → ~1-2h overlap
        int overlap = hardFilterService.calculateOverlapHours(
                "Asia/Kolkata", "America/New_York");
        assertThat(overlap).isLessThanOrEqualTo(3);

        // UTC+5:30 vs UTC+9 = 3.5h diff → ~8-9h overlap
        int japanOverlap = hardFilterService.calculateOverlapHours(
                "Asia/Kolkata", "Asia/Tokyo");
        assertThat(japanOverlap).isGreaterThanOrEqualTo(8);
    }

    @Test
    @DisplayName("Should pass all when no filters applied")
    void apply_noFilters_shouldPassAll() {
        List<ScoredCandidate> candidates = List.of(
                ScoredCandidate.builder().userId(openUserId).finalScore(0.9).build()
        );

        // availability defaults to OPEN, so only open user passes
        List<ScoredCandidate> result = hardFilterService.apply(
                candidates, null, null, 0, requesterId);

        assertThat(result).hasSize(1);
    }
}
