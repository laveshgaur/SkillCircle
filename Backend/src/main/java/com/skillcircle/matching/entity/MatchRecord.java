package com.skillcircle.matching.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Persisted match result. Stores the outcome of a match search
 * so users can review, accept, or dismiss matches later.
 */
@Entity
@Table(name = "match_results")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "requester_id", nullable = false)
    private UUID requesterId;

    @Column(name = "matched_user_id", nullable = false)
    private UUID matchedUserId;

    @Column(name = "semantic_score")
    private Double semanticScore;

    @Column(name = "collab_score")
    private Double collabScore;

    @Column(name = "goal_score")
    private Double goalScore;

    @Column(name = "final_score")
    private Double finalScore;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private MatchStatus status = MatchStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public enum MatchStatus {
        PENDING,
        ACCEPTED,
        DISMISSED
    }
}
