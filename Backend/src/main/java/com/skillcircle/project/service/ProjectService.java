package com.skillcircle.project.service;

import com.skillcircle.auth.entity.User;
import com.skillcircle.auth.repository.UserRepository;
import com.skillcircle.community.service.CommunityService;
import com.skillcircle.exception.BadRequestException;
import com.skillcircle.exception.ResourceNotFoundException;
import com.skillcircle.project.dto.*;
import com.skillcircle.project.entity.*;
import com.skillcircle.project.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for project and task management with Kanban-style task boards.
 * Automatically creates a linked community space for each project.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final TeamMembershipRepository membershipRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final CommunityService communityService;

    // ===================== Project CRUD =====================

    @Transactional
    public ProjectResponse createProject(User user, CreateProjectRequest request) {
        ProjectVisibility visibility = ProjectVisibility.PUBLIC;
        if (request.getVisibility() != null) {
            try { visibility = ProjectVisibility.valueOf(request.getVisibility().toUpperCase()); }
            catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid visibility: " + request.getVisibility());
            }
        }

        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .ownerId(user.getId())
                .status(ProjectStatus.ACTIVE)
                .visibility(visibility)
                .githubRepoUrl(request.getGithubRepoUrl())
                .build();
        project = projectRepository.save(project);

        // Auto-add owner as OWNER member
        TeamMembership ownership = TeamMembership.builder()
                .projectId(project.getId())
                .userId(user.getId())
                .role(TeamRole.OWNER)
                .build();
        membershipRepository.save(ownership);

        // Auto-create linked community space
        communityService.createProjectSpace(project.getId(), project.getName(), user.getId());

        log.info("Project '{}' created by {} with linked space", project.getName(), user.getUsername());
        return toProjectResponse(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> listUserProjects(UUID userId) {
        return projectRepository.findByTeamMember(userId)
                .stream().map(this::toProjectResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(UUID projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
        return toProjectResponse(project);
    }

    @Transactional
    public ProjectResponse updateProject(User user, UUID projectId, UpdateProjectRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        verifyProjectAccess(user.getId(), projectId, TeamRole.ADMIN);

        if (request.getName() != null) project.setName(request.getName());
        if (request.getDescription() != null) project.setDescription(request.getDescription());
        if (request.getGithubRepoUrl() != null) project.setGithubRepoUrl(request.getGithubRepoUrl());
        if (request.getStatus() != null) {
            try { project.setStatus(ProjectStatus.valueOf(request.getStatus().toUpperCase())); }
            catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid status: " + request.getStatus());
            }
        }
        if (request.getVisibility() != null) {
            try { project.setVisibility(ProjectVisibility.valueOf(request.getVisibility().toUpperCase())); }
            catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid visibility: " + request.getVisibility());
            }
        }

        project = projectRepository.save(project);
        log.info("Project {} updated by {}", projectId, user.getUsername());
        return toProjectResponse(project);
    }

    // ===================== Team Management =====================

    @Transactional
    public ProjectResponse.MemberResponse addMember(User user, UUID projectId, AddMemberRequest request) {
        verifyProjectAccess(user.getId(), projectId, TeamRole.ADMIN);

        if (membershipRepository.existsByProjectIdAndUserId(projectId, request.getUserId())) {
            throw new BadRequestException("User is already a team member");
        }

        if (!userRepository.existsById(request.getUserId())) {
            throw new ResourceNotFoundException("User", "id", request.getUserId());
        }

        TeamRole role = TeamRole.MEMBER;
        if (request.getRole() != null) {
            try { role = TeamRole.valueOf(request.getRole().toUpperCase()); }
            catch (IllegalArgumentException e) { role = TeamRole.MEMBER; }
        }
        if (role == TeamRole.OWNER) {
            throw new BadRequestException("Cannot assign OWNER role — transfer ownership instead");
        }

        TeamMembership membership = TeamMembership.builder()
                .projectId(projectId)
                .userId(request.getUserId())
                .role(role)
                .build();
        membership = membershipRepository.save(membership);

        String username = userRepository.findById(request.getUserId())
                .map(User::getUsername).orElse("unknown");

        log.info("Added {} as {} to project {}", username, role, projectId);
        return ProjectResponse.MemberResponse.builder()
                .userId(membership.getUserId())
                .username(username)
                .role(membership.getRole().name())
                .joinedAt(membership.getJoinedAt())
                .build();
    }

    @Transactional
    public void removeMember(User user, UUID projectId, UUID memberId) {
        verifyProjectAccess(user.getId(), projectId, TeamRole.ADMIN);

        TeamMembership membership = membershipRepository.findByProjectIdAndUserId(projectId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("TeamMembership", "userId", memberId));

        if (membership.getRole() == TeamRole.OWNER) {
            throw new BadRequestException("Cannot remove the project owner");
        }

        membershipRepository.deleteByProjectIdAndUserId(projectId, memberId);
        log.info("Removed user {} from project {}", memberId, projectId);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse.MemberResponse> listMembers(UUID projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project", "id", projectId);
        }
        return membershipRepository.findByProjectId(projectId).stream()
                .map(this::toMemberResponse).toList();
    }

    // ===================== Task CRUD =====================

    @Transactional
    public TaskResponse createTask(User user, UUID projectId, CreateTaskRequest request) {
        verifyProjectAccess(user.getId(), projectId, TeamRole.MEMBER);

        TaskPriority priority = TaskPriority.MEDIUM;
        if (request.getPriority() != null) {
            try { priority = TaskPriority.valueOf(request.getPriority().toUpperCase()); }
            catch (IllegalArgumentException e) { priority = TaskPriority.MEDIUM; }
        }

        Task task = Task.builder()
                .projectId(projectId)
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(priority)
                .assigneeId(request.getAssigneeId())
                .createdBy(user.getId())
                .dueDate(request.getDueDate())
                .build();
        task = taskRepository.save(task);

        log.info("Task '{}' created in project {} by {}", task.getTitle(), projectId, user.getUsername());
        return toTaskResponse(task);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> listTasks(UUID projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project", "id", projectId);
        }
        return taskRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
                .stream().map(this::toTaskResponse).toList();
    }

    /**
     * Kanban-style: tasks grouped by status.
     */
    @Transactional(readOnly = true)
    public Map<String, List<TaskResponse>> getKanbanBoard(UUID projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project", "id", projectId);
        }

        Map<String, List<TaskResponse>> board = new LinkedHashMap<>();
        for (TaskStatus status : TaskStatus.values()) {
            List<TaskResponse> tasks = taskRepository
                    .findByProjectIdAndStatusOrderByPriorityDesc(projectId, status)
                    .stream().map(this::toTaskResponse).toList();
            board.put(status.name(), tasks);
        }
        return board;
    }

    @Transactional
    public TaskResponse updateTaskStatus(User user, UUID taskId, String newStatus) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        verifyProjectAccess(user.getId(), task.getProjectId(), TeamRole.MEMBER);

        try {
            task.setStatus(TaskStatus.valueOf(newStatus.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status: " + newStatus
                    + ". Valid: TODO, IN_PROGRESS, REVIEW, DONE");
        }
        task = taskRepository.save(task);
        log.info("Task {} status → {} by {}", taskId, newStatus, user.getUsername());
        return toTaskResponse(task);
    }

    @Transactional
    public TaskResponse assignTask(User user, UUID taskId, UUID assigneeId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        verifyProjectAccess(user.getId(), task.getProjectId(), TeamRole.MEMBER);

        if (assigneeId != null && !membershipRepository.existsByProjectIdAndUserId(
                task.getProjectId(), assigneeId)) {
            throw new BadRequestException("Assignee must be a project member");
        }

        task.setAssigneeId(assigneeId);
        task = taskRepository.save(task);
        log.info("Task {} assigned to {} by {}", taskId, assigneeId, user.getUsername());
        return toTaskResponse(task);
    }

    // ===================== Access Control =====================

    /**
     * Verify user has at least the required role in the project.
     * OWNER > ADMIN > MEMBER
     */
    private void verifyProjectAccess(UUID userId, UUID projectId, TeamRole requiredRole) {
        TeamMembership membership = membershipRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new BadRequestException("You are not a member of this project"));

        if (membership.getRole().ordinal() > requiredRole.ordinal()) {
            throw new BadRequestException("Insufficient permissions — requires " + requiredRole + " or higher");
        }
    }

    // ===================== Response Builders =====================

    private ProjectResponse toProjectResponse(Project project) {
        Map<String, Long> statusCounts = new LinkedHashMap<>();
        for (TaskStatus s : TaskStatus.values()) {
            statusCounts.put(s.name(), taskRepository.countByProjectIdAndStatus(project.getId(), s));
        }

        String ownerUsername = userRepository.findById(project.getOwnerId())
                .map(User::getUsername).orElse("unknown");

        UUID spaceId = null;
        try {
            spaceId = communityService.getSpace(project.getId()) != null
                    ? null : null; // space lookup by project ID
        } catch (Exception ignored) {}

        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .ownerId(project.getOwnerId())
                .ownerUsername(ownerUsername)
                .status(project.getStatus().name())
                .visibility(project.getVisibility().name())
                .githubRepoUrl(project.getGithubRepoUrl())
                .memberCount(membershipRepository.countByProjectId(project.getId()))
                .taskCount(taskRepository.countByProjectId(project.getId()))
                .taskStatusCounts(statusCounts)
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }

    private ProjectResponse.MemberResponse toMemberResponse(TeamMembership m) {
        String username = userRepository.findById(m.getUserId())
                .map(User::getUsername).orElse("unknown");
        return ProjectResponse.MemberResponse.builder()
                .userId(m.getUserId())
                .username(username)
                .role(m.getRole().name())
                .joinedAt(m.getJoinedAt())
                .build();
    }

    private TaskResponse toTaskResponse(Task task) {
        String assigneeName = null;
        if (task.getAssigneeId() != null) {
            assigneeName = userRepository.findById(task.getAssigneeId())
                    .map(User::getUsername).orElse("unknown");
        }
        return TaskResponse.builder()
                .id(task.getId())
                .projectId(task.getProjectId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus().name())
                .priority(task.getPriority().name())
                .assigneeId(task.getAssigneeId())
                .assigneeUsername(assigneeName)
                .createdBy(task.getCreatedBy())
                .dueDate(task.getDueDate())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}
