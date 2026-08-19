package com.skillcircle.profile.service;

import com.skillcircle.auth.entity.User;
import com.skillcircle.exception.BadRequestException;
import com.skillcircle.exception.ResourceNotFoundException;
import com.skillcircle.profile.dto.AddSkillRequest;
import com.skillcircle.profile.dto.ProfileResponse;
import com.skillcircle.profile.dto.ProfileUpdateRequest;
import com.skillcircle.profile.entity.*;
import com.skillcircle.profile.repository.ProfileRepository;
import com.skillcircle.profile.repository.ProfileSkillRepository;
import com.skillcircle.profile.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for profile CRUD operations and skill management.
 * Profiles are auto-created on first access if they don't exist.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final ProfileSkillRepository profileSkillRepository;
    private final SkillRepository skillRepository;

    /**
     * Get or create a profile for the given user.
     */
    @Transactional
    public ProfileResponse getOrCreateProfile(User user) {
        Profile profile = profileRepository.findByUserIdWithSkills(user.getId())
                .orElseGet(() -> createDefaultProfile(user));
        return toResponse(profile, user);
    }

    /**
     * Get a profile by its ID (public view).
     */
    @Transactional(readOnly = true)
    public ProfileResponse getProfileById(UUID profileId) {
        Profile profile = profileRepository.findByIdWithSkills(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile", "id", profileId));
        User user = profile.getUser();
        return toResponse(profile, user);
    }

    /**
     * Update the authenticated user's profile.
     */
    @Transactional
    public ProfileResponse updateProfile(User user, ProfileUpdateRequest request) {
        Profile profile = profileRepository.findByUserId(user.getId())
                .orElseGet(() -> createDefaultProfile(user));

        // Apply partial updates — only non-null fields
        if (request.getDisplayName() != null) profile.setDisplayName(request.getDisplayName());
        if (request.getBio() != null) profile.setBio(request.getBio());
        if (request.getGoals() != null) profile.setGoals(request.getGoals());
        if (request.getTimezone() != null) profile.setTimezone(request.getTimezone());
        if (request.getGithubUsername() != null) profile.setGithubUsername(request.getGithubUsername());
        if (request.getLinkedinUrl() != null) profile.setLinkedinUrl(request.getLinkedinUrl());
        if (request.getPortfolioUrl() != null) profile.setPortfolioUrl(request.getPortfolioUrl());

        if (request.getGoalType() != null) {
            profile.setGoalType(parseEnum(GoalType.class, request.getGoalType(), "goal type"));
        }
        if (request.getExperienceLevel() != null) {
            profile.setExperienceLevel(parseEnum(ExperienceLevel.class, request.getExperienceLevel(), "experience level"));
        }
        if (request.getAvailability() != null) {
            profile.setAvailability(parseEnum(Availability.class, request.getAvailability(), "availability"));
        }

        // Invalidate embedding on profile change (will be recomputed on next match)
        profile.setEmbeddingVectorId(null);
        profile.setEmbeddingUpdatedAt(null);

        profile = profileRepository.save(profile);
        log.info("Profile updated for user: {}", user.getUsername());

        return toResponse(profileRepository.findByIdWithSkills(profile.getId()).orElse(profile), user);
    }

    /**
     * Add a skill to the authenticated user's profile.
     */
    @Transactional
    public ProfileResponse addSkill(User user, AddSkillRequest request) {
        Profile profile = profileRepository.findByUserId(user.getId())
                .orElseGet(() -> createDefaultProfile(user));

        Skill skill = skillRepository.findById(request.getSkillId())
                .orElseThrow(() -> new ResourceNotFoundException("Skill", "id", request.getSkillId()));

        if (profileSkillRepository.existsByProfileIdAndSkillId(profile.getId(), skill.getId())) {
            throw new BadRequestException("Skill '" + skill.getName() + "' is already added to your profile");
        }

        Proficiency proficiency = Proficiency.INTERMEDIATE;
        if (request.getProficiency() != null) {
            proficiency = parseEnum(Proficiency.class, request.getProficiency(), "proficiency");
        }

        ProfileSkill profileSkill = ProfileSkill.builder()
                .profileId(profile.getId())
                .skillId(skill.getId())
                .proficiency(proficiency)
                .build();
        profileSkillRepository.save(profileSkill);

        // Invalidate embedding
        profile.setEmbeddingVectorId(null);
        profile.setEmbeddingUpdatedAt(null);
        profileRepository.save(profile);

        log.info("Skill '{}' added to profile of user: {}", skill.getName(), user.getUsername());
        return toResponse(profileRepository.findByIdWithSkills(profile.getId()).orElse(profile), user);
    }

    /**
     * Remove a skill from the authenticated user's profile.
     */
    @Transactional
    public ProfileResponse removeSkill(User user, UUID skillId) {
        Profile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Profile", "userId", user.getId()));

        if (!profileSkillRepository.existsByProfileIdAndSkillId(profile.getId(), skillId)) {
            throw new BadRequestException("Skill is not in your profile");
        }

        profileSkillRepository.deleteByProfileIdAndSkillId(profile.getId(), skillId);

        // Invalidate embedding
        profile.setEmbeddingVectorId(null);
        profile.setEmbeddingUpdatedAt(null);
        profileRepository.save(profile);

        log.info("Skill removed from profile of user: {}", user.getUsername());
        return toResponse(profileRepository.findByIdWithSkills(profile.getId()).orElse(profile), user);
    }

    // ===================== Internal Helpers =====================

    private Profile createDefaultProfile(User user) {
        Profile profile = Profile.builder()
                .user(user)
                .displayName(user.getUsername())
                .availability(Availability.OPEN)
                .build();
        return profileRepository.save(profile);
    }

    private ProfileResponse toResponse(Profile profile, User user) {
        List<ProfileResponse.SkillResponse> skills = profile.getProfileSkills().stream()
                .map(ps -> ProfileResponse.SkillResponse.builder()
                        .id(ps.getSkill().getId())
                        .name(ps.getSkill().getName())
                        .category(ps.getSkill().getCategory() != null
                                ? ps.getSkill().getCategory().name() : null)
                        .proficiency(ps.getProficiency().name())
                        .build())
                .toList();

        return ProfileResponse.builder()
                .id(profile.getId())
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .displayName(profile.getDisplayName())
                .bio(profile.getBio())
                .goals(profile.getGoals())
                .goalType(profile.getGoalType() != null ? profile.getGoalType().name() : null)
                .experienceLevel(profile.getExperienceLevel() != null
                        ? profile.getExperienceLevel().name() : null)
                .timezone(profile.getTimezone())
                .availability(profile.getAvailability().name())
                .githubUsername(profile.getGithubUsername())
                .linkedinUrl(profile.getLinkedinUrl())
                .portfolioUrl(profile.getPortfolioUrl())
                .skills(skills)
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    private <T extends Enum<T>> T parseEnum(Class<T> enumClass, String value, String fieldName) {
        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid " + fieldName + ": '" + value + "'");
        }
    }
}
