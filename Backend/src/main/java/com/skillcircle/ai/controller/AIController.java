package com.skillcircle.ai.controller;

import com.skillcircle.ai.service.SkillExtractionService;
import com.skillcircle.ai.service.ThreadSummarizationService;
import com.skillcircle.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for AI-powered services.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "AI Services", description = "Thread summarization and skill extraction")
public class AIController {

    private final ThreadSummarizationService summarizationService;
    private final SkillExtractionService skillExtractionService;

    @PostMapping("/threads/{threadId}/summarize")
    @Operation(summary = "AI-summarize a discussion thread")
    public ResponseEntity<ApiResponse<Map<String, String>>> summarizeThread(
            @PathVariable UUID threadId) {
        String summary = summarizationService.summarizeThread(threadId);
        return ResponseEntity.ok(ApiResponse.success("Thread summarized",
                Map.of("summary", summary)));
    }

    @PostMapping("/ai/extract-skills")
    @Operation(summary = "Extract skills from text content (e.g., README)")
    public ResponseEntity<ApiResponse<List<String>>> extractSkills(
            @RequestBody Map<String, String> request) {
        String content = request.getOrDefault("content", "");
        List<String> skills = skillExtractionService.extractSkillsFromReadme(content);
        return ResponseEntity.ok(ApiResponse.success(
                skills.size() + " skills extracted", skills));
    }
}
