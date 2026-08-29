package com.skillcircle.community.controller;

import com.skillcircle.ai.service.ThreadSummarizationService;
import com.skillcircle.auth.entity.User;
import com.skillcircle.auth.repository.UserRepository;
import com.skillcircle.community.dto.ChatMessage;
import com.skillcircle.community.dto.MessageResponse;
import com.skillcircle.community.service.CommunityService;
import com.skillcircle.community.service.PresenceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private CommunityService communityService;
    @Mock private PresenceService presenceService;
    @Mock private UserRepository userRepository;
    @Mock private ThreadSummarizationService threadSummarizationService;

    @InjectMocks private ChatController controller;

    @Test
    @DisplayName("sendMessage persists, broadcasts, and triggers auto-summarize")
    void sendMessage_triggersAutoSummarize() {
        UUID threadId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        User sender = User.builder().id(senderId).username("alice").build();

        ChatMessage msg = new ChatMessage();
        msg.setThreadId(threadId.toString());
        msg.setSenderId(senderId.toString());
        msg.setContent("hello");
        msg.setMessageType("TEXT");

        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(communityService.sendMessage(sender, threadId, "hello", "TEXT"))
                .thenReturn(mock(MessageResponse.class));

        controller.sendMessage(msg);

        verify(communityService).sendMessage(sender, threadId, "hello", "TEXT");
        verify(messagingTemplate).convertAndSend(eq("/topic/thread." + threadId), any(Object.class));
        verify(presenceService).markOnline(senderId.toString());
        verify(threadSummarizationService).checkAutoSummarize(threadId);
    }

    @Test
    @DisplayName("sendMessage ignores unknown sender and does not summarize")
    void sendMessage_unknownSender() {
        UUID senderId = UUID.randomUUID();
        ChatMessage msg = new ChatMessage();
        msg.setThreadId(UUID.randomUUID().toString());
        msg.setSenderId(senderId.toString());
        msg.setContent("hi");

        when(userRepository.findById(senderId)).thenReturn(Optional.empty());

        controller.sendMessage(msg);

        verifyNoInteractions(communityService);
        verify(threadSummarizationService, never()).checkAutoSummarize(any());
    }
}
