package com.skillcircle.community.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data @Builder @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SpaceResponse {
    private UUID id;
    private String name;
    private String description;
    private String type;
    private UUID projectId;
    private UUID createdBy;
    private long threadCount;
    private Instant createdAt;
}
