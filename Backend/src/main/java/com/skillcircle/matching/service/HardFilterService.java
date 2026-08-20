package com.skillcircle.matching.service;

import com.skillcircle.matching.dto.ScoredCandidate;
import com.skillcircle.profile.entity.Availability;
import com.skillcircle.profile.entity.GoalType;
import com.skillcircle.profile.entity.Profile;
import com.skillcircle.profile.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Stage 3: Hard Filters.
 *
 * Applies mandatory compatibility filters to eliminate unsuitable candidates:
 * - Availability status = OPEN
 * - Timezone overlap ≥ N hours (configurable)
 * - Goal type compatibility (optional filter)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HardFilterService {

    private final ProfileRepository profileRepository;

    @Value("${app.matching.timezone-overlap-hours:4}")
    private int defaultTimezoneOverlapHours;

    /**
     * Apply hard filters to a list of scored candidates.
     *
     * @param candidates         pre-sorted candidates from Stage 2
     * @param goalTypeFilter     optional goal type to require (null = no filter)
     * @param availabilityFilter optional availability to require (null = defaults to OPEN)
     * @param tzOverlapHours     minimum timezone overlap hours (null = use default)
     * @param requesterId        the requesting user's ID (for timezone comparison)
     * @return filtered list maintaining score order
     */
    public List<ScoredCandidate> apply(
            List<ScoredCandidate> candidates,
            String goalTypeFilter,
            String availabilityFilter,
            Integer tzOverlapHours,
            UUID requesterId) {

        int minOverlap = tzOverlapHours != null ? tzOverlapHours : defaultTimezoneOverlapHours;
        Availability requiredAvailability = parseAvailability(availabilityFilter);
        GoalType requiredGoalType = parseGoalType(goalTypeFilter);

        // Get requester's timezone
        Profile requesterProfile = profileRepository.findByUserId(requesterId).orElse(null);
        String requesterTz = requesterProfile != null ? requesterProfile.getTimezone() : null;

        int beforeCount = candidates.size();

        List<ScoredCandidate> filtered = candidates.stream()
                .filter(c -> passesAvailabilityFilter(c.getUserId(), requiredAvailability))
                .filter(c -> passesGoalTypeFilter(c.getUserId(), requiredGoalType))
                .filter(c -> passesTimezoneFilter(c.getUserId(), requesterTz, minOverlap))
                .toList();

        log.debug("Hard filters: {} → {} candidates (availability={}, goalType={}, tzOverlap≥{}h)",
                beforeCount, filtered.size(), requiredAvailability, requiredGoalType, minOverlap);

        return filtered;
    }

    // ===================== Individual Filters =====================

    private boolean passesAvailabilityFilter(UUID userId, Availability required) {
        if (required == null) return true;

        Optional<Profile> profile = profileRepository.findByUserId(userId);
        return profile.map(p -> p.getAvailability() == required).orElse(false);
    }

    private boolean passesGoalTypeFilter(UUID userId, GoalType required) {
        if (required == null) return true;

        Optional<Profile> profile = profileRepository.findByUserId(userId);
        return profile.map(p -> p.getGoalType() == required).orElse(true);
    }

    private boolean passesTimezoneFilter(UUID userId, String requesterTz, int minOverlapHours) {
        if (requesterTz == null || requesterTz.isBlank() || minOverlapHours <= 0) {
            return true; // Can't filter without timezone data
        }

        Optional<Profile> profile = profileRepository.findByUserId(userId);
        if (profile.isEmpty() || profile.get().getTimezone() == null) {
            return true; // Don't exclude users without timezone set
        }

        return calculateOverlapHours(requesterTz, profile.get().getTimezone()) >= minOverlapHours;
    }

    /**
     * Calculate the number of overlapping "business hours" (9am–9pm) between two timezones.
     * Returns a value between 0 and 12.
     */
    int calculateOverlapHours(String tz1, String tz2) {
        try {
            ZoneId zone1 = ZoneId.of(tz1);
            ZoneId zone2 = ZoneId.of(tz2);

            ZonedDateTime now1 = ZonedDateTime.now(zone1);
            ZonedDateTime now2 = ZonedDateTime.now(zone2);

            int offsetDiffMinutes = (now1.getOffset().getTotalSeconds() - now2.getOffset().getTotalSeconds()) / 60;
            int offsetDiffHours = Math.abs(offsetDiffMinutes / 60);

            // Assuming 12-hour active window (9am–9pm), overlap = max(0, 12 - offset_diff)
            return Math.max(0, 12 - offsetDiffHours);
        } catch (Exception e) {
            log.warn("Failed to calculate timezone overlap between '{}' and '{}': {}",
                    tz1, tz2, e.getMessage());
            return 12; // On error, don't exclude
        }
    }

    // ===================== Parsing Helpers =====================

    private Availability parseAvailability(String value) {
        if (value == null || value.isBlank()) return Availability.OPEN; // default filter
        try {
            return Availability.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Availability.OPEN;
        }
    }

    private GoalType parseGoalType(String value) {
        if (value == null || value.isBlank()) return null; // no filter
        try {
            return GoalType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
