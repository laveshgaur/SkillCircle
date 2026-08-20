package com.skillcircle.matching.repository;

import com.skillcircle.matching.entity.CollaborationEdge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CollaborationEdgeRepository extends JpaRepository<CollaborationEdge, UUID> {

    /**
     * Find all collaboration edges involving a user (as either user A or B).
     */
    @Query("SELECT e FROM CollaborationEdge e WHERE e.userAId = :userId OR e.userBId = :userId")
    List<CollaborationEdge> findByUserId(UUID userId);

    /**
     * Find direct collaboration edges between two specific users.
     */
    @Query("SELECT e FROM CollaborationEdge e WHERE " +
            "(e.userAId = :userA AND e.userBId = :userB) OR " +
            "(e.userAId = :userB AND e.userBId = :userA)")
    List<CollaborationEdge> findEdgesBetween(UUID userA, UUID userB);

    boolean existsByUserAIdAndUserBIdAndSourceAndSourceRef(
            UUID userAId, UUID userBId, String source, String sourceRef);
}
