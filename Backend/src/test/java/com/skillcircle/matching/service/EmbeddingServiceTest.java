package com.skillcircle.matching.service;

import com.skillcircle.auth.entity.AuthProvider;
import com.skillcircle.auth.entity.Role;
import com.skillcircle.auth.entity.User;
import com.skillcircle.profile.entity.*;
import com.skillcircle.profile.repository.ProfileRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmbeddingServiceTest {

    @Mock private ProfileRepository profileRepository;
    @Mock private EmbeddingClient embeddingClient;
    @Mock private VectorDBClient vectorDBClient;

    @InjectMocks private EmbeddingService embeddingService;

    private User testUser;
    private Profile testProfile;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("alice@example.com")
                .username("alice")
                .oauthProvider(AuthProvider.LOCAL)
                .role(Role.USER)
                .isActive(true)
                .build();

        testProfile = Profile.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .displayName("Alice")
                .bio("Full-stack Java developer")
                .goals("Build collaborative open-source tools")
                .goalType(GoalType.BUILDING)
                .experienceLevel(ExperienceLevel.ADVANCED)
                .timezone("Asia/Kolkata")
                .availability(Availability.OPEN)
                .profileSkills(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("buildProfileText should concatenate profile fields")
    void buildProfileText_shouldConcatenateFields() {
        String text = embeddingService.buildProfileText(testProfile);

        assertThat(text).contains("Bio: Full-stack Java developer");
        assertThat(text).contains("Goals: Build collaborative open-source tools");
        assertThat(text).contains("Experience: ADVANCED");
        assertThat(text).contains("Goal Type: BUILDING");
    }

    @Test
    @DisplayName("buildProfileText with empty profile should return default")
    void buildProfileText_emptyProfile_shouldReturnDefault() {
        Profile emptyProfile = Profile.builder()
                .user(testUser)
                .availability(Availability.OPEN)
                .profileSkills(new ArrayList<>())
                .build();

        String text = embeddingService.buildProfileText(emptyProfile);
        assertThat(text).isEqualTo("Developer profile");
    }

    @Test
    @DisplayName("getOrComputeVector should compute and store embedding")
    void getOrComputeVector_shouldComputeAndStore() {
        float[] mockVector = new float[]{0.1f, 0.2f, 0.3f};

        when(profileRepository.findByUserIdWithSkills(testUser.getId()))
                .thenReturn(Optional.of(testProfile));
        when(embeddingClient.embed(any())).thenReturn(mockVector);
        when(profileRepository.save(any())).thenReturn(testProfile);

        float[] result = embeddingService.getOrComputeVector(testUser.getId());

        verify(embeddingClient).embed(any(String.class));
        verify(vectorDBClient).upsert(eq(testUser.getId().toString()), eq(mockVector), any());
        verify(profileRepository).save(testProfile);
        assertThat(testProfile.getEmbeddingVectorId()).isEqualTo(testUser.getId().toString());
        assertThat(testProfile.getEmbeddingUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("getOrComputeVector should skip recompute when fresh")
    void getOrComputeVector_freshEmbedding_shouldSkip() {
        testProfile.setEmbeddingVectorId(testUser.getId().toString());
        testProfile.setEmbeddingUpdatedAt(java.time.Instant.now()); // fresh

        when(profileRepository.findByUserIdWithSkills(testUser.getId()))
                .thenReturn(Optional.of(testProfile));
        when(embeddingClient.getDimensions()).thenReturn(1536);

        float[] result = embeddingService.getOrComputeVector(testUser.getId());

        verify(embeddingClient, never()).embed(any());
        verify(vectorDBClient, never()).upsert(any(), any(), any());
    }
}
