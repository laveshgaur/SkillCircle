package com.skillcircle.auth.service;

import com.skillcircle.auth.dto.LoginRequest;
import com.skillcircle.auth.dto.RegisterRequest;
import com.skillcircle.auth.entity.AuthProvider;
import com.skillcircle.auth.entity.Role;
import com.skillcircle.auth.entity.User;
import com.skillcircle.auth.repository.UserRepository;
import com.skillcircle.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService — registration, login, and token refresh logic.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User savedUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@example.com");
        registerRequest.setUsername("testuser");
        registerRequest.setPassword("password123");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        savedUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .username("testuser")
                .passwordHash("$2a$10$encoded")
                .oauthProvider(AuthProvider.LOCAL)
                .role(Role.USER)
                .isActive(true)
                .build();
    }

    // ===================== Registration Tests =====================

    @Test
    @DisplayName("Registration should succeed with valid data")
    void register_shouldSucceed() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encoded");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateAccessToken(any())).thenReturn("access.token.here");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh.token.here");
        when(jwtService.extractJti(anyString())).thenReturn("jti-123");
        when(jwtService.getAccessTokenExpirySeconds()).thenReturn(900L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        var response = authService.register(registerRequest);

        assertThat(response.getAccessToken()).isEqualTo("access.token.here");
        assertThat(response.getRefreshToken()).isEqualTo("refresh.token.here");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getUser().getEmail()).isEqualTo("test@example.com");
        assertThat(response.getUser().getRole()).isEqualTo("USER");

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Registration should fail with duplicate email")
    void register_shouldFailWithDuplicateEmail() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Email is already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Registration should fail with duplicate username")
    void register_shouldFailWithDuplicateUsername() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Username is already taken");

        verify(userRepository, never()).save(any());
    }

    // ===================== Login Tests =====================

    @Test
    @DisplayName("Login should succeed with valid credentials")
    void login_shouldSucceed() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(savedUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);
        when(jwtService.generateAccessToken(any())).thenReturn("access.token.here");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh.token.here");
        when(jwtService.extractJti(anyString())).thenReturn("jti-456");
        when(jwtService.getAccessTokenExpirySeconds()).thenReturn(900L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        var response = authService.login(loginRequest);

        assertThat(response.getAccessToken()).isEqualTo("access.token.here");
        assertThat(response.getUser().getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Login should fail with invalid credentials")
    void login_shouldFailWithBadCredentials() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);
    }

    // ===================== Logout Tests =====================

    @Test
    @DisplayName("Logout should blacklist access token and revoke refresh token")
    void logout_shouldBlacklistAndRevoke() {
        when(jwtService.isTokenExpired("access.token")).thenReturn(false);
        when(jwtService.getAccessTokenExpirySeconds()).thenReturn(900L);
        when(jwtService.extractJti("refresh.token")).thenReturn("jti-789");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        authService.logout("access.token", "refresh.token");

        verify(valueOperations).set(eq("token_blacklist:access.token"), eq("1"), anyLong(), any());
        verify(redisTemplate).delete("refresh_token:jti-789");
    }
}
