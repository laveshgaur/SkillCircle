package com.skillcircle.profile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

/**
 * Join entity between Profile and Skill with proficiency level.
 * Uses a composite primary key (profile_id, skill_id).
 */
@Entity
@Table(name = "profile_skills")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(ProfileSkill.ProfileSkillId.class)
public class ProfileSkill {

    @Id
    @Column(name = "profile_id")
    private UUID profileId;

    @Id
    @Column(name = "skill_id")
    private UUID skillId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", insertable = false, updatable = false)
    private Profile profile;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "skill_id", insertable = false, updatable = false)
    private Skill skill;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    @Builder.Default
    private Proficiency proficiency = Proficiency.INTERMEDIATE;

    /**
     * Composite key class for ProfileSkill.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfileSkillId implements Serializable {
        private UUID profileId;
        private UUID skillId;
    }
}
