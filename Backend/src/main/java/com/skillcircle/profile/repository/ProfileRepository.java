package com.skillcircle.profile.repository;

import com.skillcircle.profile.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, UUID> {

    Optional<Profile> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    @Query("SELECT p FROM Profile p LEFT JOIN FETCH p.profileSkills ps LEFT JOIN FETCH ps.skill WHERE p.user.id = :userId")
    Optional<Profile> findByUserIdWithSkills(UUID userId);

    @Query("SELECT p FROM Profile p LEFT JOIN FETCH p.profileSkills ps LEFT JOIN FETCH ps.skill WHERE p.id = :id")
    Optional<Profile> findByIdWithSkills(UUID id);
}
