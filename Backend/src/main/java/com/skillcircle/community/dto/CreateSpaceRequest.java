package com.skillcircle.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateSpaceRequest {
    @NotBlank(message = "Space name is required")
    @Size(max = 200, message = "Space name must be at most 200 characters")
    private String name;

    @Size(max = 2000)
    private String description;

    private String type; // PUBLIC, PRIVATE, PROJECT — defaults to PUBLIC
}
