package com.skillcircle.profile.entity;

import com.skillcircle.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Developer profile containing biographical info, goals, skills,
 * and matching-relevant metadata (timezone, availability, embedding ref).
 *
 * One-to-one with User. The embedding_vector_id links to the Qdrant vector DB
 * entry used by the matching engine.
 */
@Entity
@Table(name = "profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column(name = "display_name", length = 150)
    private String displayName;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(columnDefinition = "TEXT")
    private String goals;

    @Enumerated(EnumType.STRING)
    @Column(name = "goal_type", length = 30)
    private GoalType goalType;

    @Enumerated(EnumType.STRING)
    @Column(name = "experience_level", length = 30)
    private ExperienceLevel experienceLevel;

    @Column(length = 50)
    private String timezone;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    @Builder.Default
    private Availability availability = Availability.OPEN;

    @Column(name = "github_username", length = 100)
    private String githubUsername;

    @Column(name = "linkedin_url")
    private String linkedinUrl;

    @Column(name = "portfolio_url")
    private String portfolioUrl;

    @Column(name = "embedding_vector_id")
    private String embeddingVectorId;

    @Column(name = "embedding_updated_at")
    private Instant embeddingUpdatedAt;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProfileSkill> profileSkills = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    // ===================== Helper Methods =====================

    /**
     * Get comma-separated skill names for embedding text generation.
     */
    public String getSkillNames() {
        return profileSkills.stream()
                .map(ps -> ps.getSkill().getName())
                .collect(Collectors.joining(", "));
    }

    /**
     * Check if the embedding is fresh (less than 24 hours old).
     */
    public boolean isEmbeddingFresh() {
        if (embeddingVectorId == null || embeddingUpdatedAt == null) {
            return false;
        }
        return embeddingUpdatedAt.isAfter(Instant.now().minusSeconds(86400));
    }
}
