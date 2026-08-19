package com.skillcircle.profile.service;

import com.skillcircle.auth.entity.AuthProvider;
import com.skillcircle.auth.entity.Role;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock private ProfileRepository profileRepository;
    @Mock private ProfileSkillRepository profileSkillRepository;
    @Mock private SkillRepository skillRepository;

    @InjectMocks private ProfileService profileService;

    private User testUser;
    private Profile testProfile;
    private Skill testSkill;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .username("testuser")
                .oauthProvider(AuthProvider.LOCAL)
                .role(Role.USER)
                .isActive(true)
                .build();

        testProfile = Profile.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .displayName("testuser")
                .availability(Availability.OPEN)
                .profileSkills(new ArrayList<>())
                .build();

        testSkill = Skill.builder()
                .id(UUID.randomUUID())
                .name("Java")
                .category(SkillCategory.LANGUAGE)
                .build();
    }

    // ===================== Get/Create Profile =====================

    @Test
    @DisplayName("Should return existing profile")
    void getOrCreateProfile_shouldReturnExisting() {
        when(profileRepository.findByUserIdWithSkills(testUser.getId()))
                .thenReturn(Optional.of(testProfile));

        ProfileResponse response = profileService.getOrCreateProfile(testUser);

        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(response.getAvailability()).isEqualTo("OPEN");
        verify(profileRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should create profile if none exists")
    void getOrCreateProfile_shouldCreateNew() {
        when(profileRepository.findByUserIdWithSkills(testUser.getId()))
                .thenReturn(Optional.empty());
        when(profileRepository.save(any(Profile.class))).thenReturn(testProfile);

        ProfileResponse response = profileService.getOrCreateProfile(testUser);

        assertThat(response.getDisplayName()).isEqualTo("testuser");
        verify(profileRepository).save(any(Profile.class));
    }

    // ===================== Update Profile =====================

    @Test
    @DisplayName("Should update profile fields")
    void updateProfile_shouldUpdateFields() {
        when(profileRepository.findByUserId(testUser.getId()))
                .thenReturn(Optional.of(testProfile));
        when(profileRepository.save(any(Profile.class))).thenReturn(testProfile);
        when(profileRepository.findByIdWithSkills(testProfile.getId()))
                .thenReturn(Optional.of(testProfile));

        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setBio("Full-stack developer");
        request.setGoalType("BUILDING");
        request.setExperienceLevel("ADVANCED");

        ProfileResponse response = profileService.updateProfile(testUser, request);

        assertThat(testProfile.getBio()).isEqualTo("Full-stack developer");
        assertThat(testProfile.getGoalType()).isEqualTo(GoalType.BUILDING);
        assertThat(testProfile.getExperienceLevel()).isEqualTo(ExperienceLevel.ADVANCED);
    }

    @Test
    @DisplayName("Should reject invalid goal type")
    void updateProfile_shouldRejectInvalidGoalType() {
        when(profileRepository.findByUserId(testUser.getId()))
                .thenReturn(Optional.of(testProfile));

        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setGoalType("INVALID_TYPE");

        assertThatThrownBy(() -> profileService.updateProfile(testUser, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid goal type");
    }

    // ===================== Add Skill =====================

    @Test
    @DisplayName("Should add skill to profile")
    void addSkill_shouldAddSuccessfully() {
        when(profileRepository.findByUserId(testUser.getId()))
                .thenReturn(Optional.of(testProfile));
        when(skillRepository.findById(testSkill.getId()))
                .thenReturn(Optional.of(testSkill));
        when(profileSkillRepository.existsByProfileIdAndSkillId(
                testProfile.getId(), testSkill.getId()))
                .thenReturn(false);
        when(profileRepository.save(any(Profile.class))).thenReturn(testProfile);
        when(profileRepository.findByIdWithSkills(testProfile.getId()))
                .thenReturn(Optional.of(testProfile));

        AddSkillRequest request = new AddSkillRequest();
        request.setSkillId(testSkill.getId());
        request.setProficiency("ADVANCED");

        profileService.addSkill(testUser, request);

        verify(profileSkillRepository).save(any(ProfileSkill.class));
    }

    @Test
    @DisplayName("Should reject duplicate skill")
    void addSkill_shouldRejectDuplicate() {
        when(profileRepository.findByUserId(testUser.getId()))
                .thenReturn(Optional.of(testProfile));
        when(skillRepository.findById(testSkill.getId()))
                .thenReturn(Optional.of(testSkill));
        when(profileSkillRepository.existsByProfileIdAndSkillId(
                testProfile.getId(), testSkill.getId()))
                .thenReturn(true);

        AddSkillRequest request = new AddSkillRequest();
        request.setSkillId(testSkill.getId());

        assertThatThrownBy(() -> profileService.addSkill(testUser, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already added");
    }

    @Test
    @DisplayName("Should reject non-existent skill")
    void addSkill_shouldRejectNonExistent() {
        UUID fakeId = UUID.randomUUID();
        when(profileRepository.findByUserId(testUser.getId()))
                .thenReturn(Optional.of(testProfile));
        when(skillRepository.findById(fakeId)).thenReturn(Optional.empty());

        AddSkillRequest request = new AddSkillRequest();
        request.setSkillId(fakeId);

        assertThatThrownBy(() -> profileService.addSkill(testUser, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ===================== Remove Skill =====================

    @Test
    @DisplayName("Should remove skill from profile")
    void removeSkill_shouldRemoveSuccessfully() {
        when(profileRepository.findByUserId(testUser.getId()))
                .thenReturn(Optional.of(testProfile));
        when(profileSkillRepository.existsByProfileIdAndSkillId(
                testProfile.getId(), testSkill.getId()))
                .thenReturn(true);
        when(profileRepository.save(any(Profile.class))).thenReturn(testProfile);
        when(profileRepository.findByIdWithSkills(testProfile.getId()))
                .thenReturn(Optional.of(testProfile));

        profileService.removeSkill(testUser, testSkill.getId());

        verify(profileSkillRepository).deleteByProfileIdAndSkillId(
                testProfile.getId(), testSkill.getId());
    }

    @Test
    @DisplayName("Should reject removing a skill not in profile")
    void removeSkill_shouldRejectNotInProfile() {
        when(profileRepository.findByUserId(testUser.getId()))
                .thenReturn(Optional.of(testProfile));
        when(profileSkillRepository.existsByProfileIdAndSkillId(any(), any()))
                .thenReturn(false);

        assertThatThrownBy(() -> profileService.removeSkill(testUser, testSkill.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not in your profile");
    }
}
