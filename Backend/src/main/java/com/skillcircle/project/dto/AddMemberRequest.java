package com.skillcircle.project.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AddMemberRequest {
    @NotNull(message = "User ID is required")
    private UUID userId;

    private String role; // ADMIN or MEMBER (defaults to MEMBER)
}
