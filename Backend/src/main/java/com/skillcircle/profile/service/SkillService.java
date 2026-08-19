package com.skillcircle.profile.service;

import com.skillcircle.profile.dto.ProfileResponse;
import com.skillcircle.profile.entity.Skill;
import com.skillcircle.profile.entity.SkillCategory;
import com.skillcircle.profile.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for skill catalog operations: search, autocomplete, and listing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkillService {

    private final SkillRepository skillRepository;

    /**
     * Autocomplete skills by prefix (for the skill picker UI).
     */
    @Transactional(readOnly = true)
    public List<ProfileResponse.SkillResponse> autocomplete(String prefix) {
        return skillRepository.findByNameAutocomplete(prefix).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Search skills by partial name match.
     */
    @Transactional(readOnly = true)
    public List<ProfileResponse.SkillResponse> search(String query) {
        return skillRepository.searchByName(query).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * List all skills, optionally filtered by category.
     */
    @Transactional(readOnly = true)
    public List<ProfileResponse.SkillResponse> listSkills(String category) {
        List<Skill> skills;
        if (category != null && !category.isBlank()) {
            try {
                SkillCategory cat = SkillCategory.valueOf(category.toUpperCase());
                skills = skillRepository.findByCategory(cat);
            } catch (IllegalArgumentException e) {
                skills = skillRepository.findAll();
            }
        } else {
            skills = skillRepository.findAll();
        }
        return skills.stream().map(this::toResponse).toList();
    }

    private ProfileResponse.SkillResponse toResponse(Skill skill) {
        return ProfileResponse.SkillResponse.builder()
                .id(skill.getId())
                .name(skill.getName())
                .category(skill.getCategory() != null ? skill.getCategory().name() : null)
                .build();
    }
}
