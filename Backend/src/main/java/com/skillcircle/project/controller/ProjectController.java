package com.skillcircle.project.controller;

import com.skillcircle.auth.entity.User;
import com.skillcircle.common.dto.ApiResponse;
import com.skillcircle.project.dto.*;
import com.skillcircle.project.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Projects & Tasks", description = "Project management and Kanban task boards")
public class ProjectController {

    private final ProjectService projectService;

    // ===================== Projects =====================

    @GetMapping("/projects")
    @Operation(summary = "List current user's projects")
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> listProjects(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(
                projectService.listUserProjects(user.getId())));
    }

    @PostMapping("/projects")
    @Operation(summary = "Create a new project")
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateProjectRequest request) {
        ProjectResponse project = projectService.createProject(user, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Project created", project));
    }

    @GetMapping("/projects/{id}")
    @Operation(summary = "Get project details")
    public ResponseEntity<ApiResponse<ProjectResponse>> getProject(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(projectService.getProject(id)));
    }

    @PutMapping("/projects/{id}")
    @Operation(summary = "Update project")
    public ResponseEntity<ApiResponse<ProjectResponse>> updateProject(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @RequestBody UpdateProjectRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Project updated",
                projectService.updateProject(user, id, request)));
    }

    // ===================== Team Members =====================

    @GetMapping("/projects/{id}/members")
    @Operation(summary = "List project members")
    public ResponseEntity<ApiResponse<List<ProjectResponse.MemberResponse>>> listMembers(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(projectService.listMembers(id)));
    }

    @PostMapping("/projects/{id}/members")
    @Operation(summary = "Add team member")
    public ResponseEntity<ApiResponse<ProjectResponse.MemberResponse>> addMember(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @Valid @RequestBody AddMemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Member added",
                        projectService.addMember(user, id, request)));
    }

    @DeleteMapping("/projects/{id}/members/{memberId}")
    @Operation(summary = "Remove team member")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @PathVariable UUID memberId) {
        projectService.removeMember(user, id, memberId);
        return ResponseEntity.ok(ApiResponse.success("Member removed", null));
    }

    // ===================== Tasks =====================

    @GetMapping("/projects/{id}/tasks")
    @Operation(summary = "List all tasks in a project")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> listTasks(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(projectService.listTasks(id)));
    }

    @GetMapping("/projects/{id}/kanban")
    @Operation(summary = "Get Kanban board (tasks grouped by status)")
    public ResponseEntity<ApiResponse<Map<String, List<TaskResponse>>>> getKanban(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(projectService.getKanbanBoard(id)));
    }

    @PostMapping("/projects/{id}/tasks")
    @Operation(summary = "Create a task")
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @Valid @RequestBody CreateTaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Task created",
                        projectService.createTask(user, id, request)));
    }

    @PatchMapping("/tasks/{taskId}/status")
    @Operation(summary = "Change task status (Kanban move)")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTaskStatus(
            @AuthenticationPrincipal User user,
            @PathVariable UUID taskId,
            @RequestParam String status) {
        return ResponseEntity.ok(ApiResponse.success(
                projectService.updateTaskStatus(user, taskId, status)));
    }

    @PatchMapping("/tasks/{taskId}/assign")
    @Operation(summary = "Assign task to a member")
    public ResponseEntity<ApiResponse<TaskResponse>> assignTask(
            @AuthenticationPrincipal User user,
            @PathVariable UUID taskId,
            @RequestParam(required = false) UUID assigneeId) {
        return ResponseEntity.ok(ApiResponse.success(
                projectService.assignTask(user, taskId, assigneeId)));
    }
}
