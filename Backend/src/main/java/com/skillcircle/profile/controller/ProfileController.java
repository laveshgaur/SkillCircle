package com.skillcircle.profile.controller;

import com.skillcircle.auth.entity.User;
import com.skillcircle.common.dto.ApiResponse;
import com.skillcircle.profile.dto.AddSkillRequest;
import com.skillcircle.profile.dto.ProfileResponse;
import com.skillcircle.profile.dto.ProfileUpdateRequest;
import com.skillcircle.profile.service.GitHubSyncService;
import com.skillcircle.profile.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for profile and skill management.
 */
@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
@Tag(name = "Profiles", description = "Profile CRUD and skill management")
public class ProfileController {

    private final ProfileService profileService;
    private final GitHubSyncService gitHubSyncService;

    @GetMapping("/me")
    @Operation(summary = "Get current user's profile (creates if not exists)")
    public ResponseEntity<ApiResponse<ProfileResponse>> getMyProfile(
            @AuthenticationPrincipal User user) {
        ProfileResponse profile = profileService.getOrCreateProfile(user);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a profile by ID (public)")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(
            @PathVariable UUID id) {
        ProfileResponse profile = profileService.getProfileById(id);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user's profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ProfileUpdateRequest request) {
        ProfileResponse profile = profileService.updateProfile(user, request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated", profile));
    }

    @PostMapping("/me/skills")
    @Operation(summary = "Add a skill to current user's profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> addSkill(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AddSkillRequest request) {
        ProfileResponse profile = profileService.addSkill(user, request);
        return ResponseEntity.ok(ApiResponse.success("Skill added", profile));
    }

    @DeleteMapping("/me/skills/{skillId}")
    @Operation(summary = "Remove a skill from current user's profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> removeSkill(
            @AuthenticationPrincipal User user,
            @PathVariable UUID skillId) {
        ProfileResponse profile = profileService.removeSkill(user, skillId);
        return ResponseEntity.ok(ApiResponse.success("Skill removed", profile));
    }

    @PostMapping("/me/sync-github")
    @Operation(summary = "Sync skills from GitHub repositories")
    public ResponseEntity<ApiResponse<Map<String, Object>>> syncGitHub(
            @AuthenticationPrincipal User user) {
        int skillsAdded = gitHubSyncService.syncGitHub(user);
        return ResponseEntity.ok(ApiResponse.success("GitHub sync complete",
                Map.of("skillsAdded", skillsAdded)));
    }
}
