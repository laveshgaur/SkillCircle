package com.skillcircle.matching.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Records an edge in the collaboration graph between two users.
 * Used by Stage 2 (Collaborative Filtering) to boost match scores
 * for users who have co-contributed on shared repos/projects.
 */
@Entity
@Table(name = "collaboration_edges",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"user_a_id", "user_b_id", "source", "source_ref"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollaborationEdge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_a_id", nullable = false)
    private UUID userAId;

    @Column(name = "user_b_id", nullable = false)
    private UUID userBId;

    @Column(nullable = false, length = 50)
    private String source; // 'github_repo', 'skillcircle_project', 'manual'

    @Column(name = "source_ref")
    private String sourceRef; // e.g., repo URL

    @Column
    @Builder.Default
    private Float strength = 1.0f;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
