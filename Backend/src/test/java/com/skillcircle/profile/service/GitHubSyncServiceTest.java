package com.skillcircle.profile.service;

import com.skillcircle.profile.entity.Profile;
import com.skillcircle.profile.entity.Skill;
import com.skillcircle.profile.repository.ProfileRepository;
import com.skillcircle.profile.repository.ProfileSkillRepository;
import com.skillcircle.profile.repository.SkillRepository;
import com.skillcircle.ai.service.SkillExtractionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GitHubSyncServiceTest {

    @Mock private ProfileRepository profileRepository;
    @Mock private SkillRepository skillRepository;
    @Mock private ProfileSkillRepository profileSkillRepository;
    @Mock private SkillExtractionService skillExtractionService;

    @InjectMocks private GitHubSyncService service;

    @Test
    @DisplayName("addSkillsFromReadme adds each newly-extracted skill to the profile")
    void addSkillsFromReadme_addsNewSkills() {
        Profile profile = Profile.builder().id(UUID.randomUUID()).build();

        when(skillExtractionService.extractSkillsFromReadme("readme text"))
                .thenReturn(List.of("React", "Docker"));
        when(skillRepository.findByNameIgnoreCase(any())).thenReturn(Optional.empty());
        when(skillRepository.save(any())).thenAnswer(inv -> {
            Skill s = inv.getArgument(0);
            return Skill.builder().id(UUID.randomUUID())
                    .name(s.getName()).category(s.getCategory()).build();
        });
        when(profileSkillRepository.existsByProfileIdAndSkillId(any(), any())).thenReturn(false);

        int added = service.addSkillsFromReadme(profile, "readme text");

        assertThat(added).isEqualTo(2);
        verify(profileSkillRepository, times(2)).save(any());
    }

    @Test
    @DisplayName("addSkillsFromReadme skips skills already on the profile")
    void addSkillsFromReadme_skipsExisting() {
        Profile profile = Profile.builder().id(UUID.randomUUID()).build();
        Skill react = Skill.builder().id(UUID.randomUUID()).name("React").build();

        when(skillExtractionService.extractSkillsFromReadme(any()))
                .thenReturn(List.of("React"));
        when(skillRepository.findByNameIgnoreCase("React")).thenReturn(Optional.of(react));
        when(profileSkillRepository.existsByProfileIdAndSkillId(profile.getId(), react.getId()))
                .thenReturn(true);

        int added = service.addSkillsFromReadme(profile, "uses React");

        assertThat(added).isZero();
        verify(profileSkillRepository, never()).save(any());
    }

    @Test
    @DisplayName("addSkillsFromReadme returns zero when nothing is extracted")
    void addSkillsFromReadme_noSkills() {
        Profile profile = Profile.builder().id(UUID.randomUUID()).build();
        when(skillExtractionService.extractSkillsFromReadme(any())).thenReturn(List.of());

        int added = service.addSkillsFromReadme(profile, "prose with no tech");

        assertThat(added).isZero();
        verifyNoInteractions(profileSkillRepository);
    }
}
