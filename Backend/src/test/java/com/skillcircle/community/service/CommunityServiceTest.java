package com.skillcircle.community.service;

import com.skillcircle.auth.entity.AuthProvider;
import com.skillcircle.auth.entity.Role;
import com.skillcircle.auth.entity.User;
import com.skillcircle.auth.repository.UserRepository;
import com.skillcircle.community.dto.*;
import com.skillcircle.community.entity.*;
import com.skillcircle.community.entity.Thread;
import com.skillcircle.community.repository.*;
import com.skillcircle.exception.BadRequestException;
import com.skillcircle.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommunityServiceTest {

    @Mock private SpaceRepository spaceRepository;
    @Mock private ThreadRepository threadRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private CommunityService communityService;

    private User testUser;
    private Space testSpace;
    private Thread testThread;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .username("testuser")
                .oauthProvider(AuthProvider.LOCAL)
                .role(Role.USER)
                .isActive(true)
                .build();

        testSpace = Space.builder()
                .id(UUID.randomUUID())
                .name("Java Devs")
                .description("A space for Java developers")
                .type(SpaceType.PUBLIC)
                .createdBy(testUser.getId())
                .createdAt(Instant.now())
                .build();

        testThread = Thread.builder()
                .id(UUID.randomUUID())
                .spaceId(testSpace.getId())
                .title("Spring Boot Tips")
                .createdBy(testUser.getId())
                .isPinned(false)
                .createdAt(Instant.now())
                .build();
    }

    // ===================== Space Tests =====================

    @Test
    @DisplayName("Should create a public space")
    void createSpace_shouldCreatePublic() {
        when(spaceRepository.save(any(Space.class))).thenReturn(testSpace);
        lenient().when(threadRepository.countBySpaceId(any())).thenReturn(0L);

        CreateSpaceRequest request = new CreateSpaceRequest();
        request.setName("Java Devs");
        request.setDescription("A space for Java developers");

        SpaceResponse response = communityService.createSpace(testUser, request);

        assertThat(response.getName()).isEqualTo("Java Devs");
        assertThat(response.getType()).isEqualTo("PUBLIC");
        verify(spaceRepository).save(any(Space.class));
    }

    @Test
    @DisplayName("Should reject invalid space type")
    void createSpace_shouldRejectInvalidType() {
        CreateSpaceRequest request = new CreateSpaceRequest();
        request.setName("Test");
        request.setType("INVALID");

        assertThatThrownBy(() -> communityService.createSpace(testUser, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid space type");
    }

    @Test
    @DisplayName("Should list public spaces")
    void listSpaces_shouldReturnPublicSpaces() {
        when(spaceRepository.findByTypeInOrderByCreatedAtDesc(any())).thenReturn(List.of(testSpace));
        lenient().when(threadRepository.countBySpaceId(any())).thenReturn(0L);

        List<SpaceResponse> spaces = communityService.listSpaces();

        assertThat(spaces).hasSize(1);
        assertThat(spaces.get(0).getName()).isEqualTo("Java Devs");
    }

    @Test
    @DisplayName("Should get space by ID")
    void getSpace_shouldReturnSpace() {
        when(spaceRepository.findById(testSpace.getId())).thenReturn(Optional.of(testSpace));
        lenient().when(threadRepository.countBySpaceId(any())).thenReturn(0L);

        SpaceResponse response = communityService.getSpace(testSpace.getId());

        assertThat(response.getId()).isEqualTo(testSpace.getId());
    }

    @Test
    @DisplayName("Should throw when space not found")
    void getSpace_shouldThrowNotFound() {
        UUID fakeId = UUID.randomUUID();
        when(spaceRepository.findById(fakeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> communityService.getSpace(fakeId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should auto-create project space")
    void createProjectSpace_shouldCreateLinkedSpace() {
        UUID projectId = UUID.randomUUID();
        when(spaceRepository.findByProjectId(projectId)).thenReturn(Optional.empty());
        when(spaceRepository.save(any(Space.class))).thenAnswer(inv -> {
            Space s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            s.setCreatedAt(Instant.now());
            return s;
        });
        lenient().when(threadRepository.countBySpaceId(any())).thenReturn(0L);

        SpaceResponse response = communityService.createProjectSpace(
                projectId, "My Project", testUser.getId());

        assertThat(response.getType()).isEqualTo("PROJECT");
        verify(spaceRepository).save(any(Space.class));
    }

    // ===================== Thread Tests =====================

    @Test
    @DisplayName("Should create a thread in a space")
    void createThread_shouldCreateSuccessfully() {
        when(spaceRepository.existsById(testSpace.getId())).thenReturn(true);
        when(threadRepository.save(any(Thread.class))).thenReturn(testThread);
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        lenient().when(messageRepository.countByThreadId(any())).thenReturn(0L);

        CreateThreadRequest request = new CreateThreadRequest();
        request.setTitle("Spring Boot Tips");

        ThreadResponse response = communityService.createThread(testUser, testSpace.getId(), request);

        assertThat(response.getTitle()).isEqualTo("Spring Boot Tips");
        assertThat(response.getCreatedByUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Should reject thread in non-existent space")
    void createThread_shouldRejectMissingSpace() {
        UUID fakeId = UUID.randomUUID();
        when(spaceRepository.existsById(fakeId)).thenReturn(false);

        CreateThreadRequest request = new CreateThreadRequest();
        request.setTitle("Test");

        assertThatThrownBy(() -> communityService.createThread(testUser, fakeId, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should pin a thread")
    void pinThread_shouldPinSuccessfully() {
        when(threadRepository.findById(testThread.getId())).thenReturn(Optional.of(testThread));
        when(threadRepository.save(any(Thread.class))).thenReturn(testThread);
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        lenient().when(messageRepository.countByThreadId(any())).thenReturn(0L);

        ThreadResponse response = communityService.pinThread(testThread.getId(), true);

        assertThat(testThread.getIsPinned()).isTrue();
    }

    // ===================== Message Tests =====================

    @Test
    @DisplayName("Should send a message")
    void sendMessage_shouldPersistAndReturn() {
        Message savedMsg = Message.builder()
                .id(UUID.randomUUID())
                .threadId(testThread.getId())
                .senderId(testUser.getId())
                .content("Hello world!")
                .messageType(MessageType.TEXT)
                .createdAt(Instant.now())
                .build();

        when(threadRepository.existsById(testThread.getId())).thenReturn(true);
        when(messageRepository.save(any(Message.class))).thenReturn(savedMsg);

        MessageResponse response = communityService.sendMessage(
                testUser, testThread.getId(), "Hello world!", "TEXT");

        assertThat(response.getContent()).isEqualTo("Hello world!");
        assertThat(response.getSenderUsername()).isEqualTo("testuser");
        assertThat(response.getMessageType()).isEqualTo("TEXT");
    }

    @Test
    @DisplayName("Should paginate messages")
    void getMessages_shouldReturnPaginated() {
        Message msg = Message.builder()
                .id(UUID.randomUUID())
                .threadId(testThread.getId())
                .senderId(testUser.getId())
                .content("Test message")
                .messageType(MessageType.TEXT)
                .createdAt(Instant.now())
                .build();

        Page<Message> page = new PageImpl<>(List.of(msg));
        when(threadRepository.existsById(testThread.getId())).thenReturn(true);
        when(messageRepository.findByThreadIdOrderByCreatedAtDesc(
                eq(testThread.getId()), any(PageRequest.class))).thenReturn(page);
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

        Page<MessageResponse> result = communityService.getMessages(testThread.getId(), 0, 50);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getContent()).isEqualTo("Test message");
    }

    @Test
    @DisplayName("Should reject message to non-existent thread")
    void sendMessage_shouldRejectMissingThread() {
        UUID fakeId = UUID.randomUUID();
        when(threadRepository.existsById(fakeId)).thenReturn(false);

        assertThatThrownBy(() -> communityService.sendMessage(testUser, fakeId, "Hi", null))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
