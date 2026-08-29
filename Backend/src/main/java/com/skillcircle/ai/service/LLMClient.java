package com.skillcircle.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Generic LLM client supporting OpenAI-compatible APIs (GPT-4o-mini, etc.).
 *
 * Provides a simple chat completion interface for:
 * - Thread summarization
 * - Skill extraction from GitHub content
 *
 * Gracefully returns fallback responses when the API key is not configured.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LLMClient {

    @Value("${app.openai.api-key:}")
    private String apiKey;

    @Value("${app.ai.chat-model:gpt-4o-mini}")
    private String chatModel;

    @Value("${app.ai.max-tokens:1024}")
    private int maxTokens;

    /**
     * Send a chat completion request.
     *
     * @param systemPrompt the system instruction
     * @param userMessage  the user message / content to process
     * @return the assistant's response text
     */
    @SuppressWarnings("unchecked")
    public String chatCompletion(String systemPrompt, String userMessage) {
        if (!isAvailable()) {
            log.warn("OpenAI API key not configured — returning empty response");
            return "";
        }

        try {
            RestClient client = RestClient.builder()
                    .baseUrl("https://api.openai.com/v1")
                    .defaultHeader("Authorization", "Bearer " + apiKey)
                    .defaultHeader("Content-Type", "application/json")
                    .build();

            Map<String, Object> requestBody = Map.of(
                    "model", chatModel,
                    "max_tokens", maxTokens,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userMessage)
                    )
            );

            Map<String, Object> response = client.post()
                    .uri("/chat/completions")
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("choices")) {
                log.error("Invalid LLM response: {}", response);
                return "";
            }

            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String content = (String) message.get("content");

            log.debug("LLM response received ({} chars)", content.length());
            return content.trim();

        } catch (Exception e) {
            log.error("LLM API call failed: {}", e.getMessage());
            return "";
        }
    }

    /**
     * @return true if the API key is configured
     */
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }
}
