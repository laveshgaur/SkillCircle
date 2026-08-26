package com.skillcircle.project.repository;

import com.skillcircle.project.entity.TeamMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamMembershipRepository extends JpaRepository<TeamMembership, TeamMembership.TeamMembershipId> {

    List<TeamMembership> findByProjectId(UUID projectId);

    Optional<TeamMembership> findByProjectIdAndUserId(UUID projectId, UUID userId);

    boolean existsByProjectIdAndUserId(UUID projectId, UUID userId);

    void deleteByProjectIdAndUserId(UUID projectId, UUID userId);

    long countByProjectId(UUID projectId);
}
