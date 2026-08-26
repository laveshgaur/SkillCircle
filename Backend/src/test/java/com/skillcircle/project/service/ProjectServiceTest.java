package com.skillcircle.project.service;

import com.skillcircle.auth.entity.AuthProvider;
import com.skillcircle.auth.entity.Role;
import com.skillcircle.auth.entity.User;
import com.skillcircle.auth.repository.UserRepository;
import com.skillcircle.community.service.CommunityService;
import com.skillcircle.exception.BadRequestException;
import com.skillcircle.exception.ResourceNotFoundException;
import com.skillcircle.project.dto.*;
import com.skillcircle.project.entity.*;
import com.skillcircle.project.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private TeamMembershipRepository membershipRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private UserRepository userRepository;
    @Mock private CommunityService communityService;

    @InjectMocks private ProjectService projectService;

    private User testUser;
    private Project testProject;
    private TeamMembership ownerMembership;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("alice@example.com")
                .username("alice")
                .oauthProvider(AuthProvider.LOCAL)
                .role(Role.USER)
                .isActive(true)
                .build();

        testProject = Project.builder()
                .id(UUID.randomUUID())
                .name("SkillCircle")
                .description("AI dev matching")
                .ownerId(testUser.getId())
                .status(ProjectStatus.ACTIVE)
                .visibility(ProjectVisibility.PUBLIC)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        ownerMembership = TeamMembership.builder()
                .projectId(testProject.getId())
                .userId(testUser.getId())
                .role(TeamRole.OWNER)
                .build();
    }

    // ===================== Project CRUD =====================

    @Test
    @DisplayName("Should create project with auto-space")
    void createProject_shouldCreateWithSpace() {
        when(projectRepository.save(any(Project.class))).thenReturn(testProject);
        when(membershipRepository.save(any())).thenReturn(ownerMembership);
        lenient().when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        lenient().when(taskRepository.countByProjectId(any())).thenReturn(0L);
        lenient().when(taskRepository.countByProjectIdAndStatus(any(), any())).thenReturn(0L);
        lenient().when(membershipRepository.countByProjectId(any())).thenReturn(1L);

        CreateProjectRequest request = new CreateProjectRequest();
        request.setName("SkillCircle");
        request.setDescription("AI dev matching");

        ProjectResponse response = projectService.createProject(testUser, request);

        assertThat(response.getName()).isEqualTo("SkillCircle");
        verify(communityService).createProjectSpace(any(), eq("SkillCircle"), eq(testUser.getId()));
        verify(membershipRepository).save(any()); // owner membership
    }

    @Test
    @DisplayName("Should get project by ID")
    void getProject_shouldReturn() {
        when(projectRepository.findById(testProject.getId())).thenReturn(Optional.of(testProject));
        lenient().when(userRepository.findById(any())).thenReturn(Optional.of(testUser));
        lenient().when(taskRepository.countByProjectId(any())).thenReturn(0L);
        lenient().when(taskRepository.countByProjectIdAndStatus(any(), any())).thenReturn(0L);
        lenient().when(membershipRepository.countByProjectId(any())).thenReturn(1L);

        ProjectResponse response = projectService.getProject(testProject.getId());
        assertThat(response.getId()).isEqualTo(testProject.getId());
    }

    @Test
    @DisplayName("Should throw when project not found")
    void getProject_shouldThrowNotFound() {
        UUID fakeId = UUID.randomUUID();
        when(projectRepository.findById(fakeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getProject(fakeId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should update project status")
    void updateProject_shouldUpdateStatus() {
        when(projectRepository.findById(testProject.getId())).thenReturn(Optional.of(testProject));
        when(membershipRepository.findByProjectIdAndUserId(testProject.getId(), testUser.getId()))
                .thenReturn(Optional.of(ownerMembership));
        when(projectRepository.save(any())).thenReturn(testProject);
        lenient().when(userRepository.findById(any())).thenReturn(Optional.of(testUser));
        lenient().when(taskRepository.countByProjectId(any())).thenReturn(0L);
        lenient().when(taskRepository.countByProjectIdAndStatus(any(), any())).thenReturn(0L);
        lenient().when(membershipRepository.countByProjectId(any())).thenReturn(1L);

        UpdateProjectRequest request = new UpdateProjectRequest();
        request.setStatus("COMPLETED");

        projectService.updateProject(testUser, testProject.getId(), request);
        assertThat(testProject.getStatus()).isEqualTo(ProjectStatus.COMPLETED);
    }

    // ===================== Team Management =====================

    @Test
    @DisplayName("Should add team member")
    void addMember_shouldAdd() {
        UUID newUserId = UUID.randomUUID();
        when(membershipRepository.findByProjectIdAndUserId(testProject.getId(), testUser.getId()))
                .thenReturn(Optional.of(ownerMembership));
        when(membershipRepository.existsByProjectIdAndUserId(testProject.getId(), newUserId))
                .thenReturn(false);
        when(userRepository.existsById(newUserId)).thenReturn(true);
        when(membershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(newUserId)).thenReturn(
                Optional.of(User.builder().id(newUserId).username("bob").build()));

        AddMemberRequest request = new AddMemberRequest();
        request.setUserId(newUserId);

        ProjectResponse.MemberResponse response = projectService.addMember(
                testUser, testProject.getId(), request);

        assertThat(response.getUsername()).isEqualTo("bob");
        assertThat(response.getRole()).isEqualTo("MEMBER");
    }

    @Test
    @DisplayName("Should reject duplicate member")
    void addMember_shouldRejectDuplicate() {
        UUID existingUserId = UUID.randomUUID();
        when(membershipRepository.findByProjectIdAndUserId(testProject.getId(), testUser.getId()))
                .thenReturn(Optional.of(ownerMembership));
        when(membershipRepository.existsByProjectIdAndUserId(testProject.getId(), existingUserId))
                .thenReturn(true);

        AddMemberRequest request = new AddMemberRequest();
        request.setUserId(existingUserId);

        assertThatThrownBy(() -> projectService.addMember(testUser, testProject.getId(), request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already a team member");
    }

    @Test
    @DisplayName("Should not remove project owner")
    void removeMember_shouldRejectOwnerRemoval() {
        when(membershipRepository.findByProjectIdAndUserId(testProject.getId(), testUser.getId()))
                .thenReturn(Optional.of(ownerMembership));

        assertThatThrownBy(() -> projectService.removeMember(
                testUser, testProject.getId(), testUser.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot remove the project owner");
    }

    // ===================== Task CRUD =====================

    @Test
    @DisplayName("Should create task")
    void createTask_shouldCreate() {
        Task savedTask = Task.builder()
                .id(UUID.randomUUID())
                .projectId(testProject.getId())
                .title("Implement auth")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.HIGH)
                .createdBy(testUser.getId())
                .createdAt(Instant.now())
                .build();

        when(membershipRepository.findByProjectIdAndUserId(testProject.getId(), testUser.getId()))
                .thenReturn(Optional.of(ownerMembership));
        when(taskRepository.save(any())).thenReturn(savedTask);

        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Implement auth");
        request.setPriority("HIGH");

        TaskResponse response = projectService.createTask(testUser, testProject.getId(), request);

        assertThat(response.getTitle()).isEqualTo("Implement auth");
        assertThat(response.getPriority()).isEqualTo("HIGH");
        assertThat(response.getStatus()).isEqualTo("TODO");
    }

    @Test
    @DisplayName("Should update task status")
    void updateTaskStatus_shouldTransition() {
        Task task = Task.builder()
                .id(UUID.randomUUID())
                .projectId(testProject.getId())
                .title("Test task")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .createdBy(testUser.getId())
                .build();

        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(membershipRepository.findByProjectIdAndUserId(testProject.getId(), testUser.getId()))
                .thenReturn(Optional.of(ownerMembership));
        when(taskRepository.save(any())).thenReturn(task);

        projectService.updateTaskStatus(testUser, task.getId(), "IN_PROGRESS");

        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Should reject invalid task status")
    void updateTaskStatus_shouldRejectInvalid() {
        Task task = Task.builder()
                .id(UUID.randomUUID())
                .projectId(testProject.getId())
                .title("Test")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .createdBy(testUser.getId())
                .build();

        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(membershipRepository.findByProjectIdAndUserId(testProject.getId(), testUser.getId()))
                .thenReturn(Optional.of(ownerMembership));

        assertThatThrownBy(() -> projectService.updateTaskStatus(testUser, task.getId(), "INVALID"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid status");
    }

    @Test
    @DisplayName("Should assign task to member")
    void assignTask_shouldAssign() {
        UUID assigneeId = UUID.randomUUID();
        Task task = Task.builder()
                .id(UUID.randomUUID())
                .projectId(testProject.getId())
                .title("Test")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .createdBy(testUser.getId())
                .build();

        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(membershipRepository.findByProjectIdAndUserId(testProject.getId(), testUser.getId()))
                .thenReturn(Optional.of(ownerMembership));
        when(membershipRepository.existsByProjectIdAndUserId(testProject.getId(), assigneeId))
                .thenReturn(true);
        when(taskRepository.save(any())).thenReturn(task);
        when(userRepository.findById(assigneeId)).thenReturn(
                Optional.of(User.builder().id(assigneeId).username("bob").build()));

        TaskResponse response = projectService.assignTask(testUser, task.getId(), assigneeId);

        assertThat(task.getAssigneeId()).isEqualTo(assigneeId);
    }

    @Test
    @DisplayName("Should reject assigning to non-member")
    void assignTask_shouldRejectNonMember() {
        UUID nonMemberId = UUID.randomUUID();
        Task task = Task.builder()
                .id(UUID.randomUUID())
                .projectId(testProject.getId())
                .title("Test")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .createdBy(testUser.getId())
                .build();

        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(membershipRepository.findByProjectIdAndUserId(testProject.getId(), testUser.getId()))
                .thenReturn(Optional.of(ownerMembership));
        when(membershipRepository.existsByProjectIdAndUserId(testProject.getId(), nonMemberId))
                .thenReturn(false);

        assertThatThrownBy(() -> projectService.assignTask(testUser, task.getId(), nonMemberId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must be a project member");
    }
}
