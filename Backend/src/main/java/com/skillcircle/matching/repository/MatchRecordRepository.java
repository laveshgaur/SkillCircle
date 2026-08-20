package com.skillcircle.matching.repository;

import com.skillcircle.matching.entity.MatchRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MatchRecordRepository extends JpaRepository<MatchRecord, UUID> {

    List<MatchRecord> findByRequesterIdOrderByCreatedAtDesc(UUID requesterId);

    List<MatchRecord> findByRequesterIdAndStatus(UUID requesterId, MatchRecord.MatchStatus status);

    boolean existsByRequesterIdAndMatchedUserId(UUID requesterId, UUID matchedUserId);
}
