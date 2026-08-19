package com.skillcircle.auth.service;

import com.skillcircle.auth.entity.Role;
import com.skillcircle.auth.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for JwtService — token generation, extraction, and validation.
 */
class JwtServiceTest {

    private JwtService jwtService;

    // Base64-encoded 256-bit test key
    private static final String TEST_SECRET =
            "dGhpc2lzYXZlcnlsb25nc2VjcmV0a2V5Zm9ydGVzdGluZ3B1cnBvc2VzMTIzNDU2Nzg5MA==";

    private User testUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiryMs", 900000L);  // 15 min
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpiryMs", 604800000L); // 7 days

        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .username("testuser")
                .role(Role.USER)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Should generate a valid access token")
    void generateAccessToken_shouldReturnValidToken() {
        String token = jwtService.generateAccessToken(testUser);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("testuser");
        assertThat(jwtService.extractTokenType(token)).isEqualTo("access");
        assertThat(jwtService.isTokenValid(token, testUser)).isTrue();
    }

    @Test
    @DisplayName("Should generate a valid refresh token with JTI")
    void generateRefreshToken_shouldReturnTokenWithJti() {
        String token = jwtService.generateRefreshToken(testUser);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("testuser");
        assertThat(jwtService.extractTokenType(token)).isEqualTo("refresh");
        assertThat(jwtService.extractJti(token)).isNotBlank();
        assertThat(jwtService.isTokenValid(token, testUser)).isTrue();
    }

    @Test
    @DisplayName("Refresh tokens should have unique JTIs")
    void generateRefreshToken_shouldHaveUniqueJtis() {
        String token1 = jwtService.generateRefreshToken(testUser);
        String token2 = jwtService.generateRefreshToken(testUser);

        assertThat(jwtService.extractJti(token1))
                .isNotEqualTo(jwtService.extractJti(token2));
    }

    @Test
    @DisplayName("Token validation should fail for wrong user")
    void isTokenValid_shouldFailForWrongUser() {
        String token = jwtService.generateAccessToken(testUser);

        User otherUser = User.builder()
                .id(UUID.randomUUID())
                .username("otheruser")
                .build();

        assertThat(jwtService.isTokenValid(token, otherUser)).isFalse();
    }

    @Test
    @DisplayName("Expired token should be detected")
    void isTokenExpired_shouldDetectExpiredToken() throws InterruptedException {
        // Generate token with 1ms expiry
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiryMs", 1L);

        String token = jwtService.generateAccessToken(testUser);

        // Wait for token to expire
        Thread.sleep(50);

        // isTokenValid should return false for expired tokens
        assertThat(jwtService.isTokenValid(token, testUser)).isFalse();
    }

    @Test
    @DisplayName("Access token expiry should be correct in seconds")
    void getAccessTokenExpirySeconds_shouldReturnCorrectValue() {
        assertThat(jwtService.getAccessTokenExpirySeconds()).isEqualTo(900L);
    }

    @Test
    @DisplayName("Invalid token string should fail validation gracefully")
    void isTokenValid_shouldHandleInvalidTokenGracefully() {
        assertThat(jwtService.isTokenValid("not.a.valid.token", testUser)).isFalse();
    }
}
