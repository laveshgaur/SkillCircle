package com.skillcircle.ai.service;

import com.skillcircle.auth.entity.User;
import com.skillcircle.auth.repository.UserRepository;
import com.skillcircle.community.entity.Message;
import com.skillcircle.community.entity.MessageType;
import com.skillcircle.community.entity.Thread;
import com.skillcircle.community.repository.MessageRepository;
import com.skillcircle.community.repository.ThreadRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ThreadSummarizationServiceTest {

    @Mock private LLMClient llmClient;
    @Mock private ThreadRepository threadRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private ThreadSummarizationService service;

    private Thread testThread;
    private List<Message> testMessages;

    @BeforeEach
    void setUp() {
        testThread = Thread.builder()
                .id(UUID.randomUUID())
                .spaceId(UUID.randomUUID())
                .title("Spring Boot Discussion")
                .createdBy(UUID.randomUUID())
                .createdAt(Instant.now())
                .build();

        UUID senderId = UUID.randomUUID();
        testMessages = List.of(
                Message.builder().id(UUID.randomUUID()).threadId(testThread.getId())
                        .senderId(senderId).content("What's the best way to handle transactions?")
                        .messageType(MessageType.TEXT).createdAt(Instant.now().minusSeconds(60)).build(),
                Message.builder().id(UUID.randomUUID()).threadId(testThread.getId())
                        .senderId(senderId).content("Use @Transactional on service methods")
                        .messageType(MessageType.TEXT).createdAt(Instant.now()).build()
        );
    }

    @Test
    @DisplayName("Should summarize thread with LLM")
    void summarizeThread_withLLM() {
        when(threadRepository.findById(testThread.getId())).thenReturn(Optional.of(testThread));
        when(messageRepository.findTop50ByThreadIdOrderByCreatedAtDesc(testThread.getId()))
                .thenReturn(testMessages);
        when(llmClient.isAvailable()).thenReturn(true);
        when(llmClient.chatCompletion(any(), any())).thenReturn("## Summary\nDiscussion about transactions.");
        when(threadRepository.save(any())).thenReturn(testThread);
        lenient().when(userRepository.findById(any())).thenReturn(
                Optional.of(User.builder().username("alice").build()));

        String summary = service.summarizeThread(testThread.getId());

        assertThat(summary).contains("Summary");
        assertThat(testThread.getAiSummary()).isNotNull();
        assertThat(testThread.getSummaryUpdatedAt()).isNotNull();
        verify(llmClient).chatCompletion(any(), any());
        verify(threadRepository).save(testThread);
    }

    @Test
    @DisplayName("Should use fallback when LLM unavailable")
    void summarizeThread_fallback() {
        when(threadRepository.findById(testThread.getId())).thenReturn(Optional.of(testThread));
        when(messageRepository.findTop50ByThreadIdOrderByCreatedAtDesc(testThread.getId()))
                .thenReturn(testMessages);
        when(llmClient.isAvailable()).thenReturn(false);
        when(threadRepository.save(any())).thenReturn(testThread);

        String summary = service.summarizeThread(testThread.getId());

        assertThat(summary).contains("Thread Summary");
        assertThat(summary).contains("Messages**: 2");
        verify(llmClient, never()).chatCompletion(any(), any());
    }

    @Test
    @DisplayName("Should return message for empty thread")
    void summarizeThread_emptyThread() {
        when(threadRepository.findById(testThread.getId())).thenReturn(Optional.of(testThread));
        when(messageRepository.findTop50ByThreadIdOrderByCreatedAtDesc(testThread.getId()))
                .thenReturn(List.of());

        String summary = service.summarizeThread(testThread.getId());

        assertThat(summary).isEqualTo("No messages to summarize.");
    }

    @Test
    @DisplayName("Should build conversation text correctly")
    void buildConversationText_shouldFormat() {
        when(userRepository.findById(any())).thenReturn(
                Optional.of(User.builder().username("alice").build()));

        String text = service.buildConversationText(testMessages, "Test Thread");

        assertThat(text).startsWith("Thread: Test Thread");
        assertThat(text).contains("[alice]:");
        assertThat(text).contains("transactions");
    }

    @Test
    @DisplayName("Auto-summarize should trigger after threshold")
    void checkAutoSummarize_shouldTriggerAboveThreshold() {
        when(messageRepository.countByThreadId(testThread.getId())).thenReturn(55L);
        when(threadRepository.findById(testThread.getId())).thenReturn(Optional.of(testThread));
        when(messageRepository.findTop50ByThreadIdOrderByCreatedAtDesc(testThread.getId()))
                .thenReturn(testMessages);
        when(llmClient.isAvailable()).thenReturn(false);
        when(threadRepository.save(any())).thenReturn(testThread);

        service.checkAutoSummarize(testThread.getId());

        verify(threadRepository).save(any()); // summary was saved
    }

    @Test
    @DisplayName("Auto-summarize should skip below threshold")
    void checkAutoSummarize_shouldSkipBelowThreshold() {
        when(messageRepository.countByThreadId(testThread.getId())).thenReturn(10L);

        service.checkAutoSummarize(testThread.getId());

        verify(threadRepository, never()).findById(any());
    }
}
