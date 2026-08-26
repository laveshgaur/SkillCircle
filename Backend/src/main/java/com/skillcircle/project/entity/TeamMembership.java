package com.skillcircle.project.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Join table for project team membership with composite PK.
 */
@Entity
@Table(name = "team_memberships")
@IdClass(TeamMembership.TeamMembershipId.class)
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class TeamMembership {

    @Id
    @Column(name = "project_id")
    private UUID projectId;

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    @Builder.Default
    private TeamRole role = TeamRole.MEMBER;

    @CreationTimestamp
    @Column(name = "joined_at", updatable = false)
    private Instant joinedAt;

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class TeamMembershipId implements Serializable {
        private UUID projectId;
        private UUID userId;
    }
}
