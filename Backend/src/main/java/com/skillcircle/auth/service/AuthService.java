package com.skillcircle.auth.service;

import com.skillcircle.auth.dto.AuthResponse;
import com.skillcircle.auth.dto.LoginRequest;
import com.skillcircle.auth.dto.RegisterRequest;
import com.skillcircle.auth.dto.TokenRefreshRequest;
import com.skillcircle.auth.entity.AuthProvider;
import com.skillcircle.auth.entity.Role;
import com.skillcircle.auth.entity.User;
import com.skillcircle.auth.repository.UserRepository;
import com.skillcircle.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * Core authentication service handling registration, login, token refresh, and logout.
 * Refresh tokens are tracked in Redis for one-time-use rotation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    private static final String BLACKLIST_PREFIX = "token_blacklist:";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final StringRedisTemplate redisTemplate;

    /**
     * Register a new user with email/password.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check for duplicate email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered");
        }

        // Check for duplicate username
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username is already taken");
        }

        // Create user
        User user = User.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .oauthProvider(AuthProvider.LOCAL)
                .role(Role.USER)
                .isActive(true)
                .build();

        user = userRepository.save(user);
        log.info("New user registered: {} ({})", user.getUsername(), user.getEmail());

        return buildAuthResponse(user);
    }

    /**
     * Authenticate a user with email/password and return tokens.
     */
    public AuthResponse login(LoginRequest request) {
        // Spring Security's AuthenticationManager validates credentials
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = (User) authentication.getPrincipal();
        log.info("User logged in: {}", user.getUsername());

        return buildAuthResponse(user);
    }

    /**
     * Refresh an access token using a valid refresh token.
     * Implements one-time-use rotation: the old refresh token is invalidated
     * and a new pair is issued.
     */
    public AuthResponse refreshToken(TokenRefreshRequest request) {
        String refreshToken = request.getRefreshToken();

        // Validate token type
        String tokenType = jwtService.extractTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new BadRequestException("Invalid token type — expected refresh token");
        }

        // Check if token has been used (one-time-use rotation)
        String jti = jwtService.extractJti(refreshToken);
        String redisKey = REFRESH_TOKEN_PREFIX + jti;
        if (Boolean.FALSE.equals(redisTemplate.hasKey(redisKey))) {
            throw new BadRequestException("Refresh token has been revoked or already used");
        }

        // Extract user and validate
        String username = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (!jwtService.isTokenValid(refreshToken, user)) {
            throw new BadRequestException("Refresh token is invalid or expired");
        }

        // Invalidate old refresh token (delete from Redis)
        redisTemplate.delete(redisKey);
        log.info("Token refreshed for user: {}", user.getUsername());

        return buildAuthResponse(user);
    }

    /**
     * Logout by blacklisting the current access token and revoking the refresh token.
     */
    public void logout(String accessToken, String refreshToken) {
        // Blacklist access token (store until expiry)
        try {
            if (accessToken != null && !jwtService.isTokenExpired(accessToken)) {
                String blacklistKey = BLACKLIST_PREFIX + accessToken;
                redisTemplate.opsForValue().set(blacklistKey, "1",
                        jwtService.getAccessTokenExpirySeconds(), TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.warn("Failed to blacklist access token: {}", e.getMessage());
        }

        // Revoke refresh token
        try {
            if (refreshToken != null) {
                String jti = jwtService.extractJti(refreshToken);
                redisTemplate.delete(REFRESH_TOKEN_PREFIX + jti);
            }
        } catch (Exception e) {
            log.warn("Failed to revoke refresh token: {}", e.getMessage());
        }

        log.info("User logged out");
    }

    /**
     * Check if an access token has been blacklisted (logged out).
     */
    public boolean isTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }

    // ===================== Internal Helpers =====================

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // Store refresh token JTI in Redis with TTL for one-time-use tracking
        String jti = jwtService.extractJti(refreshToken);
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + jti,
                user.getUsername(),
                7, TimeUnit.DAYS // matches refresh token expiry
        );

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirySeconds())
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .username(user.getUsername())
                        .avatarUrl(user.getAvatarUrl())
                        .role(user.getRole().name())
                        .build())
                .build();
    }
}
