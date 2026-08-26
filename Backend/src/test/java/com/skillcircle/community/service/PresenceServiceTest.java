package com.skillcircle.community.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PresenceServiceTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ZSetOperations<String, String> zSetOps;

    @InjectMocks private PresenceService presenceService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
    }

    @Test
    @DisplayName("Should mark user online")
    void markOnline_shouldAddToSortedSet() {
        presenceService.markOnline("user-123");
        verify(zSetOps).add(eq("presence:online"), eq("user-123"), anyDouble());
    }

    @Test
    @DisplayName("Should mark user offline")
    void markOffline_shouldRemoveFromSet() {
        presenceService.markOffline("user-123");
        verify(zSetOps).remove("presence:online", "user-123");
    }

    @Test
    @DisplayName("Should return online users")
    void getOnlineUsers_shouldReturnSet() {
        when(zSetOps.range("presence:online", 0, -1))
                .thenReturn(Set.of("user-1", "user-2"));

        Set<String> online = presenceService.getOnlineUsers();

        assertThat(online).containsExactlyInAnyOrder("user-1", "user-2");
        // Should clean stale entries
        verify(zSetOps).removeRangeByScore(eq("presence:online"), eq(0.0), anyDouble());
    }

    @Test
    @DisplayName("Should check single user presence")
    void isOnline_shouldReturnTrue() {
        when(zSetOps.score("presence:online", "user-123"))
                .thenReturn((double) System.currentTimeMillis());

        assertThat(presenceService.isOnline("user-123")).isTrue();
    }

    @Test
    @DisplayName("Should return false for absent user")
    void isOnline_shouldReturnFalseWhenAbsent() {
        when(zSetOps.score("presence:online", "user-404"))
                .thenReturn(null);

        assertThat(presenceService.isOnline("user-404")).isFalse();
    }

    @Test
    @DisplayName("Should handle Redis errors gracefully")
    void markOnline_shouldHandleRedisError() {
        when(redisTemplate.opsForZSet()).thenThrow(new RuntimeException("Redis down"));

        // Should not throw
        presenceService.markOnline("user-123");
    }
}
