package com.skillcircle.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateProjectRequest {
    @NotBlank(message = "Project name is required")
    @Size(max = 255)
    private String name;

    @Size(max = 5000)
    private String description;

    private String visibility; // PUBLIC or PRIVATE
    private String githubRepoUrl;
}
