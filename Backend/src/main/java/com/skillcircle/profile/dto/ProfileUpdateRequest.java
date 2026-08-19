package com.skillcircle.profile.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for creating or updating a profile.
 * All fields are optional — only provided fields are updated.
 */
@Data
public class ProfileUpdateRequest {

    @Size(max = 150, message = "Display name must be at most 150 characters")
    private String displayName;

    @Size(max = 2000, message = "Bio must be at most 2000 characters")
    private String bio;

    @Size(max = 1000, message = "Goals must be at most 1000 characters")
    private String goals;

    private String goalType;        // LEARNING, BUILDING, MENTORING, EXPLORING
    private String experienceLevel; // BEGINNER, INTERMEDIATE, ADVANCED, EXPERT
    private String timezone;        // e.g., "Asia/Kolkata"
    private String availability;    // OPEN, BUSY, CLOSED
    private String githubUsername;
    private String linkedinUrl;
    private String portfolioUrl;
}
