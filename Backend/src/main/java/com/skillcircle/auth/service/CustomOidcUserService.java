package com.skillcircle.auth.service;

import com.skillcircle.auth.entity.AuthProvider;
import com.skillcircle.auth.entity.Role;
import com.skillcircle.auth.entity.User;
import com.skillcircle.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Custom OIDC user service for OpenID Connect providers (Google).
 *
 * Google uses OIDC (not plain OAuth2), so Spring invokes OidcUserService
 * instead of DefaultOAuth2UserService. This service mirrors the logic
 * in OAuth2UserService: find-or-create the user, pre-generate JWT tokens,
 * and store them in Redis for the OAuth2SuccessHandler to pick up.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final StringRedisTemplate redisTemplate;

    private static final String OAUTH_TOKEN_PREFIX = "oauth_tokens:";

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());

        Map<String, Object> attributes = oidcUser.getAttributes();

        final String oauthId = (String) attributes.get("sub");
        final String email = (String) attributes.get("email");
        final String name = (String) attributes.get("name");
        final String avatarUrl = (String) attributes.get("picture");

        // Find existing user or create new one
        User user = userRepository.findByOauthProviderAndOauthId(provider, oauthId)
                .or(() -> {
                    // Also check by email — user may have registered with email/password first
                    if (email != null) {
                        return userRepository.findByEmail(email)
                                .map(existingUser -> {
                                    // Link the OAuth provider to the existing account
                                    existingUser.setOauthProvider(provider);
                                    existingUser.setOauthId(oauthId);
                                    if (existingUser.getAvatarUrl() == null && avatarUrl != null) {
                                        existingUser.setAvatarUrl(avatarUrl);
                                    }
                                    return userRepository.saveAndFlush(existingUser);
                                });
                    }
                    return java.util.Optional.empty();
                })
                .orElseGet(() -> createOAuthUser(provider, oauthId, email, name, avatarUrl));

        // Pre-generate tokens and store in Redis (picked up by OAuth2SuccessHandler)
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        String jti = jwtService.extractJti(refreshToken);
        redisTemplate.opsForValue().set(
                "refresh_token:" + jti,
                user.getUsername(),
                7, TimeUnit.DAYS
        );

        // Store tokens temporarily for the success handler to retrieve
        String tokenKey = OAUTH_TOKEN_PREFIX + user.getId().toString();
        redisTemplate.opsForHash().put(tokenKey, "accessToken", accessToken);
        redisTemplate.opsForHash().put(tokenKey, "refreshToken", refreshToken);
        redisTemplate.expire(tokenKey, 5, TimeUnit.MINUTES);

        log.info("OIDC user authenticated: {} via {}", user.getUsername(), provider);

        return oidcUser;
    }

    private User createOAuthUser(AuthProvider provider, String oauthId,
                                  String email, String name, String avatarUrl) {
        // Generate unique username if taken
        String username = name != null ? name.replaceAll("\\s+", "_") : "user_" + oauthId;
        int counter = 1;
        String baseUsername = username;
        while (userRepository.existsByUsername(username)) {
            username = baseUsername + counter++;
        }

        // Handle duplicate email from different providers
        String finalEmail = email;
        if (email != null && userRepository.existsByEmail(email)) {
            finalEmail = provider.name().toLowerCase() + "_" + oauthId + "@oauth.skillcircle";
        }
        if (finalEmail == null) {
            finalEmail = provider.name().toLowerCase() + "_" + oauthId + "@oauth.skillcircle";
        }

        User user = User.builder()
                .email(finalEmail)
                .username(username)
                .oauthProvider(provider)
                .oauthId(oauthId)
                .avatarUrl(avatarUrl)
                .role(Role.USER)
                .isActive(true)
                .build();

        user = userRepository.saveAndFlush(user);
        log.info("New OIDC user created: {} via {}", user.getUsername(), provider);

        return user;
    }
}
