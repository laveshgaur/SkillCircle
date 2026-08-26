package com.skillcircle.community.service;

import com.skillcircle.auth.entity.User;
import com.skillcircle.auth.repository.UserRepository;
import com.skillcircle.community.dto.*;
import com.skillcircle.community.entity.*;
import com.skillcircle.community.entity.Thread;
import com.skillcircle.community.repository.*;
import com.skillcircle.exception.BadRequestException;
import com.skillcircle.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for community space and thread management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityService {

    private final SpaceRepository spaceRepository;
    private final ThreadRepository threadRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    // ===================== Space Operations =====================

    @Transactional
    public SpaceResponse createSpace(User user, CreateSpaceRequest request) {
        SpaceType type = SpaceType.PUBLIC;
        if (request.getType() != null) {
            try {
                type = SpaceType.valueOf(request.getType().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid space type: " + request.getType());
            }
        }

        Space space = Space.builder()
                .name(request.getName())
                .description(request.getDescription())
                .type(type)
                .createdBy(user.getId())
                .build();
        space = spaceRepository.save(space);

        log.info("Space '{}' created by {}", space.getName(), user.getUsername());
        return toSpaceResponse(space);
    }

    @Transactional(readOnly = true)
    public List<SpaceResponse> listSpaces() {
        return spaceRepository.findByTypeInOrderByCreatedAtDesc(
                        List.of(SpaceType.PUBLIC, SpaceType.PROJECT))
                .stream().map(this::toSpaceResponse).toList();
    }

    @Transactional(readOnly = true)
    public SpaceResponse getSpace(UUID spaceId) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Space", "id", spaceId));
        return toSpaceResponse(space);
    }

    /**
     * Auto-create a project-linked space.
     */
    @Transactional
    public SpaceResponse createProjectSpace(UUID projectId, String projectName, UUID createdBy) {
        if (spaceRepository.findByProjectId(projectId).isPresent()) {
            return toSpaceResponse(spaceRepository.findByProjectId(projectId).get());
        }
        Space space = Space.builder()
                .name(projectName)
                .description("Discussion space for project: " + projectName)
                .type(SpaceType.PROJECT)
                .projectId(projectId)
                .createdBy(createdBy)
                .build();
        space = spaceRepository.save(space);
        log.info("Project space auto-created for project {}", projectId);
        return toSpaceResponse(space);
    }

    // ===================== Thread Operations =====================

    @Transactional
    public ThreadResponse createThread(User user, UUID spaceId, CreateThreadRequest request) {
        if (!spaceRepository.existsById(spaceId)) {
            throw new ResourceNotFoundException("Space", "id", spaceId);
        }

        Thread thread = Thread.builder()
                .spaceId(spaceId)
                .title(request.getTitle())
                .createdBy(user.getId())
                .isPinned(false)
                .build();
        thread = threadRepository.save(thread);

        log.info("Thread '{}' created in space {} by {}", thread.getTitle(), spaceId, user.getUsername());
        return toThreadResponse(thread);
    }

    @Transactional(readOnly = true)
    public Page<ThreadResponse> listThreads(UUID spaceId, int page, int size) {
        if (!spaceRepository.existsById(spaceId)) {
            throw new ResourceNotFoundException("Space", "id", spaceId);
        }
        return threadRepository.findBySpaceIdOrderByIsPinnedDescCreatedAtDesc(
                spaceId, PageRequest.of(page, size)).map(this::toThreadResponse);
    }

    @Transactional
    public ThreadResponse pinThread(UUID threadId, boolean pin) {
        Thread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new ResourceNotFoundException("Thread", "id", threadId));
        thread.setIsPinned(pin);
        thread = threadRepository.save(thread);
        return toThreadResponse(thread);
    }

    // ===================== Message Operations =====================

    @Transactional
    public MessageResponse sendMessage(User user, UUID threadId, String content, String type) {
        if (!threadRepository.existsById(threadId)) {
            throw new ResourceNotFoundException("Thread", "id", threadId);
        }

        MessageType msgType = MessageType.TEXT;
        if (type != null) {
            try { msgType = MessageType.valueOf(type.toUpperCase()); }
            catch (IllegalArgumentException ignored) {}
        }

        Message message = Message.builder()
                .threadId(threadId)
                .senderId(user.getId())
                .content(content)
                .messageType(msgType)
                .build();
        message = messageRepository.save(message);

        return toMessageResponse(message, user.getUsername());
    }

    @Transactional(readOnly = true)
    public Page<MessageResponse> getMessages(UUID threadId, int page, int size) {
        if (!threadRepository.existsById(threadId)) {
            throw new ResourceNotFoundException("Thread", "id", threadId);
        }
        return messageRepository.findByThreadIdOrderByCreatedAtDesc(
                threadId, PageRequest.of(page, size)).map(this::toMessageResponse);
    }

    // ===================== Response Builders =====================

    private SpaceResponse toSpaceResponse(Space space) {
        return SpaceResponse.builder()
                .id(space.getId())
                .name(space.getName())
                .description(space.getDescription())
                .type(space.getType().name())
                .projectId(space.getProjectId())
                .createdBy(space.getCreatedBy())
                .threadCount(threadRepository.countBySpaceId(space.getId()))
                .createdAt(space.getCreatedAt())
                .build();
    }

    private ThreadResponse toThreadResponse(Thread thread) {
        String username = userRepository.findById(thread.getCreatedBy())
                .map(User::getUsername).orElse("unknown");
        return ThreadResponse.builder()
                .id(thread.getId())
                .spaceId(thread.getSpaceId())
                .title(thread.getTitle())
                .createdBy(thread.getCreatedBy())
                .createdByUsername(username)
                .isPinned(thread.getIsPinned())
                .aiSummary(thread.getAiSummary())
                .messageCount(messageRepository.countByThreadId(thread.getId()))
                .createdAt(thread.getCreatedAt())
                .build();
    }

    private MessageResponse toMessageResponse(Message msg) {
        String username = userRepository.findById(msg.getSenderId())
                .map(User::getUsername).orElse("unknown");
        return toMessageResponse(msg, username);
    }

    private MessageResponse toMessageResponse(Message msg, String username) {
        return MessageResponse.builder()
                .id(msg.getId())
                .threadId(msg.getThreadId())
                .senderId(msg.getSenderId())
                .senderUsername(username)
                .content(msg.getContent())
                .messageType(msg.getMessageType().name())
                .createdAt(msg.getCreatedAt())
                .updatedAt(msg.getUpdatedAt())
                .build();
    }
}
