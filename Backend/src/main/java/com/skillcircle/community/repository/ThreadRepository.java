package com.skillcircle.community.repository;

import com.skillcircle.community.entity.Thread;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ThreadRepository extends JpaRepository<Thread, UUID> {

    Page<Thread> findBySpaceIdOrderByIsPinnedDescCreatedAtDesc(UUID spaceId, Pageable pageable);

    List<Thread> findBySpaceIdAndIsPinnedTrue(UUID spaceId);

    long countBySpaceId(UUID spaceId);
}
