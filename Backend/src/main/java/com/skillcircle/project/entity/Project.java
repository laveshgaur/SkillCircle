package com.skillcircle.project.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * A collaborative project owned by a user.
 */
@Entity
@Table(name = "projects")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    @Builder.Default
    private ProjectStatus status = ProjectStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private ProjectVisibility visibility = ProjectVisibility.PUBLIC;

    @Column(name = "github_repo_url")
    private String githubRepoUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
