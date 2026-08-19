package com.skillcircle.auth.service;

import com.skillcircle.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Custom UserDetailsService that loads users by email (for login)
 * or by username (for JWT validation).
 *
 * Spring Security's authentication flow calls loadUserByUsername,
 * but we use email as the login identifier. This service first
 * tries email lookup, then falls back to username.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        // Try email first (used during login), then username (used during JWT validation)
        return userRepository.findByEmail(identifier)
                .or(() -> userRepository.findByUsername(identifier))
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with identifier: " + identifier));
    }
}
