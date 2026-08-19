package com.skillcircle.auth.service;

import com.skillcircle.auth.entity.AuthProvider;
import com.skillcircle.auth.entity.Role;
import com.skillcircle.auth.entity.User;
import com.skillcircle.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Custom OAuth2 user service that handles user creation/linking for
 * GitHub and Google OAuth2 login flows.
 *
 * On first login, creates a new User record. On subsequent logins,
 * returns the existing user.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final StringRedisTemplate redisTemplate;

    private static final String OAUTH_TOKEN_PREFIX = "oauth_tokens:";

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());

        Map<String, Object> attributes = oAuth2User.getAttributes();

        final String oauthId;
        final String email;
        final String name;
        final String avatarUrl;

        if (provider == AuthProvider.GITHUB) {
            oauthId = String.valueOf(attributes.get("id"));
            String rawEmail = (String) attributes.get("email");
            name = (String) attributes.get("login");
            avatarUrl = (String) attributes.get("avatar_url");

            // GitHub may not return email in profile — use login as fallback
            email = (rawEmail != null) ? rawEmail : name + "@github.placeholder";
        } else { // GOOGLE
            oauthId = (String) attributes.get("sub");
            email = (String) attributes.get("email");
            name = (String) attributes.get("name");
            avatarUrl = (String) attributes.get("picture");
        }

        // Find existing user or create new one
        User user = userRepository.findByOauthProviderAndOauthId(provider, oauthId)
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

        log.info("OAuth2 user authenticated: {} via {}", user.getUsername(), provider);

        return oAuth2User;
    }

    private User createOAuthUser(AuthProvider provider, String oauthId,
                                  String email, String name, String avatarUrl) {
        // Generate unique username if taken
        String username = name;
        int counter = 1;
        while (userRepository.existsByUsername(username)) {
            username = name + counter++;
        }

        // Handle duplicate email from different providers
        String finalEmail = email;
        if (userRepository.existsByEmail(email)) {
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

        user = userRepository.save(user);
        log.info("New OAuth2 user created: {} via {}", user.getUsername(), provider);

        return user;
    }
}
