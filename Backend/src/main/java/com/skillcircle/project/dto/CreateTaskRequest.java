package com.skillcircle.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class CreateTaskRequest {
    @NotBlank(message = "Task title is required")
    @Size(max = 255)
    private String title;

    @Size(max = 5000)
    private String description;

    private String priority; // LOW, MEDIUM, HIGH, URGENT
    private UUID assigneeId;
    private LocalDate dueDate;
}
