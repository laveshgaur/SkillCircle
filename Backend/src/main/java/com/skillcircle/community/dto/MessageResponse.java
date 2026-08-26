package com.skillcircle.community.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data @Builder @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageResponse {
    private UUID id;
    private UUID threadId;
    private UUID senderId;
    private String senderUsername;
    private String content;
    private String messageType;
    private Instant createdAt;
    private Instant updatedAt;
}
