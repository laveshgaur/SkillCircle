package com.skillcircle.project.dto;

import lombok.Data;

@Data
public class UpdateProjectRequest {
    private String name;
    private String description;
    private String status;      // PLANNING, ACTIVE, COMPLETED, ARCHIVED
    private String visibility;  // PUBLIC, PRIVATE
    private String githubRepoUrl;
}
