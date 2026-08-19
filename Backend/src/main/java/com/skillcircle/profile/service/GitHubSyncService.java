package com.skillcircle.profile.service;

import com.skillcircle.auth.entity.User;
import com.skillcircle.exception.BadRequestException;
import com.skillcircle.profile.entity.*;
import com.skillcircle.profile.repository.ProfileRepository;
import com.skillcircle.profile.repository.ProfileSkillRepository;
import com.skillcircle.profile.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Service for syncing a user's GitHub profile data.
 * Fetches public repos and extracts languages and topics as skills.
 *
 * Uses on-demand sync triggered by the user (not automatic),
 * with results cached in the profile for 24 hours.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubSyncService {

    private static final String GITHUB_API = "https://api.github.com";

    private final ProfileRepository profileRepository;
    private final SkillRepository skillRepository;
    private final ProfileSkillRepository profileSkillRepository;

    /**
     * Sync GitHub profile data: fetch repos, extract languages/topics,
     * and add them as skills to the user's profile.
     *
     * @return number of new skills added
     */
    @Transactional
    @SuppressWarnings("unchecked")
    public int syncGitHub(User user) {
        Profile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BadRequestException(
                        "Create a profile before syncing GitHub"));

        String githubUsername = profile.getGithubUsername();
        if (githubUsername == null || githubUsername.isBlank()) {
            throw new BadRequestException(
                    "Set your GitHub username in your profile before syncing");
        }

        RestClient restClient = RestClient.builder()
                .baseUrl(GITHUB_API)
                .defaultHeader("Accept", "application/vnd.github.v3+json")
                .defaultHeader("User-Agent", "SkillCircle-App")
                .build();

        // Fetch public repos (up to 100)
        List<Map<String, Object>> repos;
        try {
            repos = restClient.get()
                    .uri("/users/{username}/repos?per_page=100&sort=updated", githubUsername)
                    .retrieve()
                    .body(List.class);
        } catch (Exception e) {
            log.error("GitHub API error for user {}: {}", githubUsername, e.getMessage());
            throw new BadRequestException(
                    "Failed to fetch GitHub repos. Verify your username is correct.");
        }

        if (repos == null || repos.isEmpty()) {
            log.info("No repos found for GitHub user: {}", githubUsername);
            return 0;
        }

        int added = 0;

        // Extract language from each repo
        for (Map<String, Object> repo : repos) {
            String language = (String) repo.get("language");
            if (language != null && !language.isBlank()) {
                added += addSkillIfNotExists(profile, language, SkillCategory.LANGUAGE);
            }

            // Extract topics
            Object topicsObj = repo.get("topics");
            if (topicsObj instanceof List<?> topics) {
                for (Object topic : topics) {
                    if (topic instanceof String topicStr && !topicStr.isBlank()) {
                        added += addSkillIfNotExists(profile, topicStr, SkillCategory.OTHER);
                    }
                }
            }
        }

        log.info("GitHub sync for {}: {} new skills added from {} repos",
                githubUsername, added, repos.size());

        return added;
    }

    /**
     * Add a skill to the profile if it doesn't already exist.
     * Creates the skill in the catalog if it's not a known skill.
     *
     * @return 1 if added, 0 if already existed
     */
    private int addSkillIfNotExists(Profile profile, String skillName, SkillCategory category) {
        // Normalize skill name
        String normalized = skillName.trim();
        if (normalized.length() > 100) return 0;

        // Find or create skill in catalog
        Skill skill = skillRepository.findByNameIgnoreCase(normalized)
                .orElseGet(() -> {
                    Skill newSkill = Skill.builder()
                            .name(normalized)
                            .category(category)
                            .build();
                    return skillRepository.save(newSkill);
                });

        // Check if already in profile
        if (profileSkillRepository.existsByProfileIdAndSkillId(profile.getId(), skill.getId())) {
            return 0;
        }

        // Add to profile
        ProfileSkill ps = ProfileSkill.builder()
                .profileId(profile.getId())
                .skillId(skill.getId())
                .proficiency(Proficiency.INTERMEDIATE)
                .build();
        profileSkillRepository.save(ps);

        return 1;
    }
}
