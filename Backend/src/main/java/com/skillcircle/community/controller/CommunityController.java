package com.skillcircle.community.controller;

import com.skillcircle.auth.entity.User;
import com.skillcircle.common.dto.ApiResponse;
import com.skillcircle.community.dto.*;
import com.skillcircle.community.service.CommunityService;
import com.skillcircle.community.service.PresenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * REST controller for community spaces, threads, and messages.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Community", description = "Spaces, threads, and messaging")
public class CommunityController {

    private final CommunityService communityService;
    private final PresenceService presenceService;

    // ===================== Spaces =====================

    @GetMapping("/spaces")
    @Operation(summary = "List public and project spaces")
    public ResponseEntity<ApiResponse<List<SpaceResponse>>> listSpaces() {
        return ResponseEntity.ok(ApiResponse.success(communityService.listSpaces()));
    }

    @PostMapping("/spaces")
    @Operation(summary = "Create a new space")
    public ResponseEntity<ApiResponse<SpaceResponse>> createSpace(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateSpaceRequest request) {
        SpaceResponse space = communityService.createSpace(user, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Space created", space));
    }

    @GetMapping("/spaces/{id}")
    @Operation(summary = "Get space details")
    public ResponseEntity<ApiResponse<SpaceResponse>> getSpace(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(communityService.getSpace(id)));
    }

    // ===================== Threads =====================

    @GetMapping("/spaces/{spaceId}/threads")
    @Operation(summary = "List threads in a space (paginated, pinned first)")
    public ResponseEntity<ApiResponse<Page<ThreadResponse>>> listThreads(
            @PathVariable UUID spaceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                communityService.listThreads(spaceId, page, size)));
    }

    @PostMapping("/spaces/{spaceId}/threads")
    @Operation(summary = "Create a thread in a space")
    public ResponseEntity<ApiResponse<ThreadResponse>> createThread(
            @AuthenticationPrincipal User user,
            @PathVariable UUID spaceId,
            @Valid @RequestBody CreateThreadRequest request) {
        ThreadResponse thread = communityService.createThread(user, spaceId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Thread created", thread));
    }

    @PatchMapping("/threads/{threadId}/pin")
    @Operation(summary = "Pin or unpin a thread")
    public ResponseEntity<ApiResponse<ThreadResponse>> pinThread(
            @PathVariable UUID threadId,
            @RequestParam boolean pin) {
        return ResponseEntity.ok(ApiResponse.success(
                communityService.pinThread(threadId, pin)));
    }

    // ===================== Messages =====================

    @GetMapping("/threads/{threadId}/messages")
    @Operation(summary = "Get messages in a thread (paginated, newest first)")
    public ResponseEntity<ApiResponse<Page<MessageResponse>>> getMessages(
            @PathVariable UUID threadId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                communityService.getMessages(threadId, page, size)));
    }

    // ===================== Presence =====================

    @GetMapping("/presence/online")
    @Operation(summary = "Get currently online user IDs")
    public ResponseEntity<ApiResponse<Set<String>>> getOnlineUsers() {
        return ResponseEntity.ok(ApiResponse.success(presenceService.getOnlineUsers()));
    }
}
