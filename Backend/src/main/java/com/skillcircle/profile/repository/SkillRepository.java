package com.skillcircle.profile.repository;

import com.skillcircle.profile.entity.Skill;
import com.skillcircle.profile.entity.SkillCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SkillRepository extends JpaRepository<Skill, UUID> {

    Optional<Skill> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    List<Skill> findByCategory(SkillCategory category);

    /**
     * Autocomplete: find skills whose name starts with the given prefix (case-insensitive).
     * Limited to 20 results for performance.
     */
    @Query("SELECT s FROM Skill s WHERE LOWER(s.name) LIKE LOWER(CONCAT(:prefix, '%')) ORDER BY s.name ASC LIMIT 20")
    List<Skill> findByNameAutocomplete(String prefix);

    /**
     * Search skills by partial name match (contains, case-insensitive).
     */
    @Query("SELECT s FROM Skill s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY s.name ASC LIMIT 20")
    List<Skill> searchByName(String query);
}
