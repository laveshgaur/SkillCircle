package com.skillcircle.community.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data @Builder @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ThreadResponse {
    private UUID id;
    private UUID spaceId;
    private String title;
    private UUID createdBy;
    private String createdByUsername;
    private Boolean isPinned;
    private String aiSummary;
    private long messageCount;
    private Instant createdAt;
}
