package com.skillcircle.profile.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Profile response DTO returned by GET endpoints.
 */
@Data
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProfileResponse {

    private UUID id;
    private UUID userId;
    private String username;
    private String email;
    private String avatarUrl;
    private String displayName;
    private String bio;
    private String goals;
    private String goalType;
    private String experienceLevel;
    private String timezone;
    private String availability;
    private String githubUsername;
    private String linkedinUrl;
    private String portfolioUrl;
    private List<SkillResponse> skills;
    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @Builder
    @AllArgsConstructor
    public static class SkillResponse {
        private UUID id;
        private String name;
        private String category;
        private String proficiency;
    }
}
