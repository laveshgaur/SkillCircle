package com.skillcircle.community.controller;

import com.skillcircle.ai.service.ThreadSummarizationService;
import com.skillcircle.auth.entity.User;
import com.skillcircle.auth.repository.UserRepository;
import com.skillcircle.community.dto.ChatMessage;
import com.skillcircle.community.dto.MessageResponse;
import com.skillcircle.community.service.CommunityService;
import com.skillcircle.community.service.PresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.UUID;

/**
 * WebSocket STOMP controller for real-time chat.
 *
 * Client sends to:    /app/chat.send, /app/chat.typing, /app/chat.stopTyping
 * Server broadcasts:  /topic/thread.{threadId}
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final CommunityService communityService;
    private final PresenceService presenceService;
    private final UserRepository userRepository;
    private final ThreadSummarizationService threadSummarizationService;

    /**
     * Handle incoming chat messages — persist and broadcast.
     */
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessage chatMessage) {
        String threadId = chatMessage.getThreadId();
        String senderId = chatMessage.getSenderId();

        // Look up user for persistence
        User sender = userRepository.findById(UUID.fromString(senderId)).orElse(null);
        if (sender == null) {
            log.warn("Unknown sender: {}", senderId);
            return;
        }

        // Persist message
        MessageResponse saved = communityService.sendMessage(
                sender,
                UUID.fromString(threadId),
                chatMessage.getContent(),
                chatMessage.getMessageType()
        );

        // Broadcast to thread subscribers
        messagingTemplate.convertAndSend(
                "/topic/thread." + threadId, saved);

        // Update presence
        presenceService.markOnline(senderId);

        // Fire-and-forget: auto-summarize the thread if it has grown large (>50 messages).
        // Runs on the aiTaskExecutor, so a slow LLM call never delays message delivery.
        try {
            threadSummarizationService.checkAutoSummarize(UUID.fromString(threadId));
        } catch (Exception e) {
            log.warn("Auto-summarize trigger failed for thread {}: {}", threadId, e.getMessage());
        }

        log.debug("Message sent to thread {} by {}", threadId, sender.getUsername());
    }

    /**
     * Handle typing indicator — broadcast without persisting.
     */
    @MessageMapping("/chat.typing")
    public void typing(@Payload ChatMessage chatMessage) {
        ChatMessage indicator = new ChatMessage();
        indicator.setThreadId(chatMessage.getThreadId());
        indicator.setSenderId(chatMessage.getSenderId());
        indicator.setSenderUsername(chatMessage.getSenderUsername());
        indicator.setAction("TYPING");

        messagingTemplate.convertAndSend(
                "/topic/thread." + chatMessage.getThreadId() + ".typing",
                indicator);
    }

    /**
     * Handle stop typing indicator.
     */
    @MessageMapping("/chat.stopTyping")
    public void stopTyping(@Payload ChatMessage chatMessage) {
        ChatMessage indicator = new ChatMessage();
        indicator.setThreadId(chatMessage.getThreadId());
        indicator.setSenderId(chatMessage.getSenderId());
        indicator.setSenderUsername(chatMessage.getSenderUsername());
        indicator.setAction("STOP_TYPING");

        messagingTemplate.convertAndSend(
                "/topic/thread." + chatMessage.getThreadId() + ".typing",
                indicator);
    }
}
