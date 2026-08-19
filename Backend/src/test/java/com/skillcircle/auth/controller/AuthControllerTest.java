package com.skillcircle.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillcircle.auth.dto.AuthResponse;
import com.skillcircle.auth.dto.LoginRequest;
import com.skillcircle.auth.dto.RegisterRequest;
import com.skillcircle.auth.filter.JwtAuthFilter;
import com.skillcircle.auth.filter.OAuth2SuccessHandler;
import com.skillcircle.auth.service.AuthService;
import com.skillcircle.auth.service.JwtService;
import com.skillcircle.auth.service.CustomUserDetailsService;
import com.skillcircle.auth.service.OAuth2UserService;
import com.skillcircle.config.SecurityConfig;
import com.skillcircle.exception.BadRequestException;
import com.skillcircle.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthController endpoints using MockMvc.
 * Tests HTTP layer: request validation, response codes, JSON structure.
 *
 * Excludes OAuth2 auto-configuration and SecurityConfig to avoid requiring
 * OAuth2 client credentials during testing. Security is effectively disabled
 * for these controller-level tests — security filter chain tests would be
 * separate integration tests.
 */
@WebMvcTest(
        controllers = AuthController.class,
        excludeAutoConfiguration = {
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class
        },
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {
                        SecurityConfig.class,
                        JwtAuthFilter.class,
                        OAuth2SuccessHandler.class,
                        OAuth2UserService.class
                }
        )
)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    private AuthResponse buildMockAuthResponse() {
        return AuthResponse.builder()
                .accessToken("mock.access.token")
                .refreshToken("mock.refresh.token")
                .tokenType("Bearer")
                .expiresIn(900)
                .user(AuthResponse.UserInfo.builder()
                        .id(UUID.randomUUID())
                        .email("test@example.com")
                        .username("testuser")
                        .role("USER")
                        .build())
                .build();
    }

    // ===================== Registration Tests =====================

    @Test
    @DisplayName("POST /register should return 201 with valid data")
    void register_shouldReturn201() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setUsername("testuser");
        request.setPassword("password123");

        when(authService.register(any())).thenReturn(buildMockAuthResponse());

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("mock.access.token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.email").value("test@example.com"));
    }

    @Test
    @DisplayName("POST /register should return 400 for missing email")
    void register_shouldReturn400ForMissingEmail() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /register should return 400 for short password")
    void register_shouldReturn400ForShortPassword() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setUsername("testuser");
        request.setPassword("short");

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /register should return 400 for duplicate email")
    void register_shouldReturn400ForDuplicateEmail() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@example.com");
        request.setUsername("testuser");
        request.setPassword("password123");

        when(authService.register(any())).thenThrow(
                new BadRequestException("Email is already registered"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Email is already registered"));
    }

    // ===================== Login Tests =====================

    @Test
    @DisplayName("POST /login should return 200 with valid credentials")
    void login_shouldReturn200() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(authService.login(any())).thenReturn(buildMockAuthResponse());

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.user.username").value("testuser"));
    }

    @Test
    @DisplayName("POST /login should return 401 for invalid credentials")
    void login_shouldReturn401ForBadCredentials() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrongpassword");

        when(authService.login(any())).thenThrow(
                new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ===================== Logout Tests =====================

    @Test
    @DisplayName("POST /logout should return 200")
    void logout_shouldReturn200() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(csrf())
                        .header("Authorization", "Bearer mock.access.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"mock.refresh.token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
