package com.skillcircle.profile.controller;

import com.skillcircle.common.dto.ApiResponse;
import com.skillcircle.profile.dto.ProfileResponse;
import com.skillcircle.profile.service.SkillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for the public skill catalog.
 * Used by the frontend skill picker for autocomplete and browsing.
 */
@RestController
@RequestMapping("/api/v1/skills")
@RequiredArgsConstructor
@Tag(name = "Skills", description = "Skill catalog search and listing")
public class SkillController {

    private final SkillService skillService;

    @GetMapping("/autocomplete")
    @Operation(summary = "Autocomplete skills by prefix (for skill picker)")
    public ResponseEntity<ApiResponse<List<ProfileResponse.SkillResponse>>> autocomplete(
            @RequestParam String q) {
        List<ProfileResponse.SkillResponse> skills = skillService.autocomplete(q);
        return ResponseEntity.ok(ApiResponse.success(skills));
    }

    @GetMapping("/search")
    @Operation(summary = "Search skills by partial name match")
    public ResponseEntity<ApiResponse<List<ProfileResponse.SkillResponse>>> search(
            @RequestParam String q) {
        List<ProfileResponse.SkillResponse> skills = skillService.search(q);
        return ResponseEntity.ok(ApiResponse.success(skills));
    }

    @GetMapping
    @Operation(summary = "List all skills, optionally filtered by category")
    public ResponseEntity<ApiResponse<List<ProfileResponse.SkillResponse>>> list(
            @RequestParam(required = false) String category) {
        List<ProfileResponse.SkillResponse> skills = skillService.listSkills(category);
        return ResponseEntity.ok(ApiResponse.success(skills));
    }
}
