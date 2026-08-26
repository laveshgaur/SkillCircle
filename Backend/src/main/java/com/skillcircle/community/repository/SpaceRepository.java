package com.skillcircle.community.repository;

import com.skillcircle.community.entity.Space;
import com.skillcircle.community.entity.SpaceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpaceRepository extends JpaRepository<Space, UUID> {

    List<Space> findByTypeOrderByCreatedAtDesc(SpaceType type);

    List<Space> findByCreatedByOrderByCreatedAtDesc(UUID createdBy);

    Optional<Space> findByProjectId(UUID projectId);

    List<Space> findByTypeInOrderByCreatedAtDesc(List<SpaceType> types);
}
