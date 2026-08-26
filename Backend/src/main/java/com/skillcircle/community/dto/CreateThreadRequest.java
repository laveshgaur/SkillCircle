package com.skillcircle.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateThreadRequest {
    @NotBlank(message = "Thread title is required")
    @Size(max = 300, message = "Title must be at most 300 characters")
    private String title;
}
