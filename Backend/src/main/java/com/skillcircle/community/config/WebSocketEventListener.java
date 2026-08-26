package com.skillcircle.community.config;

import com.skillcircle.community.service.PresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * Listens for WebSocket connect/disconnect events to update presence tracking.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final PresenceService presenceService;

    @EventListener
    public void handleWebSocketConnect(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        // User ID is set via custom header or extracted from principal
        String userId = accessor.getFirstNativeHeader("userId");
        if (userId != null) {
            presenceService.markOnline(userId);
            log.info("WebSocket connected: session={}, user={}", sessionId, userId);
        }
    }

    @EventListener
    public void handleWebSocketDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String userId = accessor.getFirstNativeHeader("userId");
        if (userId != null) {
            presenceService.markOffline(userId);
            log.info("WebSocket disconnected: session={}, user={}", sessionId, userId);
        }
    }
}
