package com.skillcircle.project.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data @Builder @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProjectResponse {
    private UUID id;
    private String name;
    private String description;
    private UUID ownerId;
    private String ownerUsername;
    private String status;
    private String visibility;
    private String githubRepoUrl;
    private long memberCount;
    private long taskCount;
    private Map<String, Long> taskStatusCounts; // Kanban summary
    private UUID spaceId; // linked community space
    private Instant createdAt;
    private Instant updatedAt;

    @Data @Builder @AllArgsConstructor
    public static class MemberResponse {
        private UUID userId;
        private String username;
        private String role;
        private Instant joinedAt;
    }
}
