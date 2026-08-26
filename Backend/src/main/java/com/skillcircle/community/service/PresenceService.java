package com.skillcircle.community.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Redis-backed online presence tracking.
 * Users are marked online with a TTL — if no heartbeat, they go offline automatically.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PresenceService {

    private static final String PRESENCE_KEY = "presence:online";
    private static final Duration HEARTBEAT_TTL = Duration.ofMinutes(2);

    private final StringRedisTemplate redisTemplate;

    /**
     * Mark a user as online (called on WebSocket connect + periodic heartbeat).
     */
    public void markOnline(String userId) {
        try {
            redisTemplate.opsForZSet().add(PRESENCE_KEY, userId,
                    System.currentTimeMillis());
            log.debug("User {} marked online", userId);
        } catch (Exception e) {
            log.warn("Redis unavailable for presence tracking: {}", e.getMessage());
        }
    }

    /**
     * Mark a user as offline (called on WebSocket disconnect).
     */
    public void markOffline(String userId) {
        try {
            redisTemplate.opsForZSet().remove(PRESENCE_KEY, userId);
            log.debug("User {} marked offline", userId);
        } catch (Exception e) {
            log.warn("Redis unavailable for presence tracking: {}", e.getMessage());
        }
    }

    /**
     * Get all currently online user IDs.
     * Cleans up stale entries older than the heartbeat TTL.
     */
    public Set<String> getOnlineUsers() {
        try {
            long cutoff = System.currentTimeMillis() - HEARTBEAT_TTL.toMillis();
            // Remove stale entries
            redisTemplate.opsForZSet().removeRangeByScore(PRESENCE_KEY, 0, cutoff);
            // Return current online users
            Set<String> online = redisTemplate.opsForZSet().range(PRESENCE_KEY, 0, -1);
            return online != null ? online : Set.of();
        } catch (Exception e) {
            log.warn("Redis unavailable for presence query: {}", e.getMessage());
            return Set.of();
        }
    }

    /**
     * Check if a specific user is online.
     */
    public boolean isOnline(String userId) {
        try {
            Double score = redisTemplate.opsForZSet().score(PRESENCE_KEY, userId);
            if (score == null) return false;
            return (System.currentTimeMillis() - score.longValue()) < HEARTBEAT_TTL.toMillis();
        } catch (Exception e) {
            return false;
        }
    }
}
