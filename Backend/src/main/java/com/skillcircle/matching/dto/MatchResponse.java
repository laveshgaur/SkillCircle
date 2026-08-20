package com.skillcircle.matching.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for a match result.
 */
@Data
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MatchResponse {

    private UUID matchId;
    private UUID userId;
    private String username;
    private String displayName;
    private String avatarUrl;
    private String bio;
    private String goalType;
    private String experienceLevel;
    private String timezone;
    private List<String> skills;

    private Double semanticScore;
    private Double collabScore;
    private Double goalScore;
    private Double finalScore;
    private String status;
    private Instant matchedAt;
}
