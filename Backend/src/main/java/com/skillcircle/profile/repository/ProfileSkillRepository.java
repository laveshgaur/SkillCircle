package com.skillcircle.profile.repository;

import com.skillcircle.profile.entity.ProfileSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProfileSkillRepository extends JpaRepository<ProfileSkill, ProfileSkill.ProfileSkillId> {

    List<ProfileSkill> findByProfileId(UUID profileId);

    Optional<ProfileSkill> findByProfileIdAndSkillId(UUID profileId, UUID skillId);

    void deleteByProfileIdAndSkillId(UUID profileId, UUID skillId);

    boolean existsByProfileIdAndSkillId(UUID profileId, UUID skillId);
}
