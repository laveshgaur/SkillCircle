package com.skillcircle.auth.repository;

import com.skillcircle.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Data access layer for User entities.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByOauthProviderAndOauthId(
            com.skillcircle.auth.entity.AuthProvider oauthProvider,
            String oauthId
    );

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);
}
