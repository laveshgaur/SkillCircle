package com.skillcircle.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket message payload for sending/receiving chat messages.
 */
@Data @NoArgsConstructor @AllArgsConstructor
public class ChatMessage {
    private String threadId;
    private String senderId;
    private String senderUsername;

    @NotBlank
    @Size(max = 10000)
    private String content;

    private String messageType; // TEXT, CODE, IMAGE, SYSTEM
    private String action;      // SEND, TYPING, STOP_TYPING
}
