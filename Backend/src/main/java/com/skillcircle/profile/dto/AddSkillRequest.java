package com.skillcircle.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

/**
 * Request body for adding a skill to a profile.
 */
@Data
public class AddSkillRequest {

    @NotNull(message = "Skill ID is required")
    private UUID skillId;

    private String proficiency; // BEGINNER, INTERMEDIATE, ADVANCED, EXPERT — defaults to INTERMEDIATE
}
