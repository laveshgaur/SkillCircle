package com.skillcircle.community.repository;

import com.skillcircle.community.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    Page<Message> findByThreadIdOrderByCreatedAtDesc(UUID threadId, Pageable pageable);

    /** Cursor-based: messages before a given timestamp. */
    List<Message> findByThreadIdAndCreatedAtBeforeOrderByCreatedAtDesc(
            UUID threadId, Instant before);

    long countByThreadId(UUID threadId);

    /** Get the N most recent messages for AI summarization. */
    List<Message> findTop50ByThreadIdOrderByCreatedAtDesc(UUID threadId);
}
