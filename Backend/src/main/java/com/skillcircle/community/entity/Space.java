package com.skillcircle.community.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * A community space — a container for threaded conversations.
 * Can be public, private, or linked to a project.
 */
@Entity
@Table(name = "spaces")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class Space {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    @Builder.Default
    private SpaceType type = SpaceType.PUBLIC;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
