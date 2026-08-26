package com.skillcircle.project.repository;

import com.skillcircle.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    List<Project> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    /** Projects where the user is a team member (any role). */
    @Query("SELECT p FROM Project p WHERE p.id IN " +
            "(SELECT tm.projectId FROM TeamMembership tm WHERE tm.userId = :userId) " +
            "ORDER BY p.updatedAt DESC")
    List<Project> findByTeamMember(UUID userId);
}
