package com.skillcircle.matching.service;

import com.skillcircle.exception.ResourceNotFoundException;
import com.skillcircle.profile.entity.Profile;
import com.skillcircle.profile.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Stage 1 service: manages the profile → text → embedding → vector DB lifecycle.
 *
 * Responsible for:
 * - Building the profile text string for embedding
 * - Computing embeddings via OpenAI
 * - Upserting vectors into Qdrant with metadata payload
 * - Freshness-based caching (24h TTL)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final ProfileRepository profileRepository;
    private final EmbeddingClient embeddingClient;
    private final VectorDBClient vectorDBClient;

    /**
     * Get or compute the embedding vector for a user.
     * Uses cached vector if fresh (<24h), otherwise recomputes.
     *
     * @param userId the user's UUID
     * @return the embedding float array
     */
    @Transactional
    public float[] getOrComputeVector(UUID userId) {
        Profile profile = profileRepository.findByUserIdWithSkills(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile", "userId", userId));

        // If embedding is fresh, return early (vector already in Qdrant)
        if (profile.isEmbeddingFresh()) {
            log.debug("Embedding is fresh for user {}, skipping recomputation", userId);
            return new float[embeddingClient.getDimensions()]; // Qdrant holds the actual vector
        }

        // Build profile text for embedding
        String profileText = buildProfileText(profile);

        // Generate embedding via OpenAI
        float[] vector = embeddingClient.embed(profileText);

        // Upsert into Qdrant with metadata for filtering
        Map<String, Object> payload = buildPayload(profile);
        vectorDBClient.upsert(userId.toString(), vector, payload);

        // Update profile with embedding reference
        profile.setEmbeddingVectorId(userId.toString());
        profile.setEmbeddingUpdatedAt(Instant.now());
        profileRepository.save(profile);

        log.info("Computed and stored embedding for user {} ({} chars → {} dims)",
                userId, profileText.length(), vector.length);

        return vector;
    }

    /**
     * Build the concatenated text string from a profile for embedding.
     * This is the core text that defines a user's semantic representation.
     */
    public String buildProfileText(Profile profile) {
        StringBuilder sb = new StringBuilder();

        String skillNames = profile.getSkillNames();
        if (skillNames != null && !skillNames.isBlank()) {
            sb.append("Skills: ").append(skillNames);
        }

        if (profile.getBio() != null && !profile.getBio().isBlank()) {
            sb.append(" | Bio: ").append(profile.getBio());
        }

        if (profile.getGoals() != null && !profile.getGoals().isBlank()) {
            sb.append(" | Goals: ").append(profile.getGoals());
        }

        if (profile.getExperienceLevel() != null) {
            sb.append(" | Experience: ").append(profile.getExperienceLevel().name());
        }

        if (profile.getGoalType() != null) {
            sb.append(" | Goal Type: ").append(profile.getGoalType().name());
        }

        String text = sb.toString().trim();
        return text.isEmpty() ? "Developer profile" : text;
    }

    /**
     * Build the Qdrant payload with filterable metadata.
     */
    private Map<String, Object> buildPayload(Profile profile) {
        return Map.of(
                "userId", profile.getUser().getId().toString(),
                "goalType", profile.getGoalType() != null ? profile.getGoalType().name() : "",
                "experienceLevel", profile.getExperienceLevel() != null
                        ? profile.getExperienceLevel().name() : "",
                "timezone", profile.getTimezone() != null ? profile.getTimezone() : "",
                "availability", profile.getAvailability().name()
        );
    }
}
