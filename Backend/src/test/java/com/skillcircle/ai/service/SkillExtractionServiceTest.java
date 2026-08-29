package com.skillcircle.ai.service;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkillExtractionServiceTest {

    @Mock private LLMClient llmClient;
    @Mock private SkillRepository skillRepository;

    @InjectMocks private SkillExtractionService service;

    @Test
    @DisplayName("Should extract skills via LLM")
    void extractSkills_withLLM() {
        when(llmClient.isAvailable()).thenReturn(true);
        when(llmClient.chatCompletion(any(), any()))
                .thenReturn("[\"React\", \"Node.js\", \"PostgreSQL\"]");

        List<String> skills = service.extractSkillsFromReadme("A React app with Node.js backend");

        assertThat(skills).containsExactly("React", "Node.js", "PostgreSQL");
    }

    @Test
    @DisplayName("Should use regex fallback when LLM unavailable")
    void extractSkills_fallback() {
        when(llmClient.isAvailable()).thenReturn(false);

        List<String> skills = service.extractSkillsFromReadme(
                "Built with React and Spring Boot, uses PostgreSQL and Docker for deployment");

        assertThat(skills).contains("React", "Spring Boot", "PostgreSQL", "Docker");
    }

    @Test
    @DisplayName("Fallback should detect skills from the seeded catalog (beyond regex patterns)")
    void extractSkills_fallbackUsesCatalog() {
        // Kafka and Jenkins are in the seeded taxonomy but NOT in the curated regex
        // patterns, so a match here proves the catalog branch actually runs.
        when(llmClient.isAvailable()).thenReturn(false);
        when(skillRepository.findAll()).thenReturn(List.of(
                Skill.builder().name("Kafka").category(SkillCategory.TOOL).build(),
                Skill.builder().name("Jenkins").category(SkillCategory.DEVOPS).build()));

        List<String> skills = service.extractSkillsFromReadme(
                "Event streaming with Kafka, continuous integration via Jenkins.");

        assertThat(skills).contains("Kafka", "Jenkins");
    }

    @Test
    @DisplayName("Should return empty for null input")
    void extractSkills_nullInput() {
        assertThat(service.extractSkillsFromReadme(null)).isEmpty();
        assertThat(service.extractSkillsFromReadme("")).isEmpty();
    }

    @Test
    @DisplayName("Should parse JSON skill array correctly")
    void parseSkillsFromJson_shouldParse() {
        assertThat(service.parseSkillsFromJson("[\"Java\", \"Python\", \"Go\"]"))
                .containsExactly("Java", "Python", "Go");
    }

    @Test
    @DisplayName("Should handle malformed JSON gracefully")
    void parseSkillsFromJson_malformed() {
        assertThat(service.parseSkillsFromJson("not json at all")).isEmpty();
        assertThat(service.parseSkillsFromJson("")).isEmpty();
        assertThat(service.parseSkillsFromJson(null)).isEmpty();
    }

    @Test
    @DisplayName("Should handle markdown-wrapped JSON response")
    void parseSkillsFromJson_markdownWrapped() {
        String response = "```json\n[\"React\", \"TypeScript\"]\n```";
        assertThat(service.parseSkillsFromJson(response))
                .containsExactly("React", "TypeScript");
    }

    @Test
    @DisplayName("Regex fallback should detect common technologies")
    void extractSkillsWithRegex_shouldDetect() {
        String readme = "This project uses Python with Django, PostgreSQL database, " +
                "deployed on AWS with Docker and Kubernetes.";

        List<String> skills = service.extractSkillsWithRegex(readme);

        assertThat(skills).contains("Python", "Django", "PostgreSQL", "Docker", "Kubernetes", "AWS");
    }

    @Test
    @DisplayName("Regex should distinguish Java from JavaScript")
    void extractSkillsWithRegex_javaVsJavascript() {
        List<String> javaSkills = service.extractSkillsWithRegex("Built with Java and Spring Boot");
        assertThat(javaSkills).contains("Java", "Spring Boot");

        List<String> noJava = service.extractSkillsWithRegex("Uses JavaScript and TypeScript");
        assertThat(noJava).doesNotContain("Java");
        assertThat(noJava).contains("TypeScript");
    }
}
