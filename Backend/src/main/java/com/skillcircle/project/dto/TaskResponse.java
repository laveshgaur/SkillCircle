package com.skillcircle.project.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data @Builder @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskResponse {
    private UUID id;
    private UUID projectId;
    private String title;
    private String description;
    private String status;
    private String priority;
    private UUID assigneeId;
    private String assigneeUsername;
    private UUID createdBy;
    private LocalDate dueDate;
    private Instant createdAt;
    private Instant updatedAt;
}
