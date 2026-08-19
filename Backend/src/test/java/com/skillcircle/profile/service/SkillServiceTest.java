package com.skillcircle.profile.service;

import com.skillcircle.profile.dto.ProfileResponse;
import com.skillcircle.profile.entity.Skill;
import com.skillcircle.profile.entity.SkillCategory;
import com.skillcircle.profile.repository.SkillRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkillServiceTest {

    @Mock private SkillRepository skillRepository;
    @InjectMocks private SkillService skillService;

    private Skill buildSkill(String name, SkillCategory category) {
        return Skill.builder()
                .id(UUID.randomUUID())
                .name(name)
                .category(category)
                .build();
    }

    @Test
    @DisplayName("Autocomplete should return matching skills")
    void autocomplete_shouldReturnMatches() {
        List<Skill> skills = List.of(
                buildSkill("Java", SkillCategory.LANGUAGE),
                buildSkill("JavaScript", SkillCategory.LANGUAGE)
        );
        when(skillRepository.findByNameAutocomplete("Ja")).thenReturn(skills);

        List<ProfileResponse.SkillResponse> results = skillService.autocomplete("Ja");

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getName()).isEqualTo("Java");
    }

    @Test
    @DisplayName("Search should return partial matches")
    void search_shouldReturnPartialMatches() {
        List<Skill> skills = List.of(buildSkill("React Native", SkillCategory.FRAMEWORK));
        when(skillRepository.searchByName("native")).thenReturn(skills);

        List<ProfileResponse.SkillResponse> results = skillService.search("native");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("React Native");
    }

    @Test
    @DisplayName("List skills should return all when no category filter")
    void listSkills_shouldReturnAll() {
        List<Skill> skills = List.of(
                buildSkill("Java", SkillCategory.LANGUAGE),
                buildSkill("Docker", SkillCategory.TOOL)
        );
        when(skillRepository.findAll()).thenReturn(skills);

        List<ProfileResponse.SkillResponse> results = skillService.listSkills(null);

        assertThat(results).hasSize(2);
    }

    @Test
    @DisplayName("List skills should filter by category")
    void listSkills_shouldFilterByCategory() {
        List<Skill> tools = List.of(buildSkill("Docker", SkillCategory.TOOL));
        when(skillRepository.findByCategory(SkillCategory.TOOL)).thenReturn(tools);

        List<ProfileResponse.SkillResponse> results = skillService.listSkills("tool");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCategory()).isEqualTo("TOOL");
    }
}
