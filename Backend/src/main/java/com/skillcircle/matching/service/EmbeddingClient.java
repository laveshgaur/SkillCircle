package com.skillcircle.matching.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Client for OpenAI's Embedding API.
 * Converts profile text into a dense vector for semantic similarity matching.
 *
 * Uses text-embedding-3-small (1536 dims) by default.
 * Falls back to a zero vector when the API key is not configured.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingClient {

    @Value("${app.openai.api-key:}")
    private String apiKey;

    @Value("${app.openai.embedding-model:text-embedding-3-small}")
    private String model;

    @Value("${app.openai.embedding-dimensions:1536}")
    private int dimensions;

    /**
     * Generate an embedding vector for the given text.
     *
     * @param text the profile text to embed
     * @return float array of dimension size
     */
    @SuppressWarnings("unchecked")
    public float[] embed(String text) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("OpenAI API key not configured — returning zero vector");
            return new float[dimensions];
        }

        try {
            RestClient client = RestClient.builder()
                    .baseUrl("https://api.openai.com/v1")
                    .defaultHeader("Authorization", "Bearer " + apiKey)
                    .defaultHeader("Content-Type", "application/json")
                    .build();

            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "input", text,
                    "dimensions", dimensions
            );

            Map<String, Object> response = client.post()
                    .uri("/embeddings")
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("data")) {
                log.error("Invalid OpenAI response: {}", response);
                return new float[dimensions];
            }

            List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
            List<Number> embedding = (List<Number>) data.get(0).get("embedding");

            float[] vector = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                vector[i] = embedding.get(i).floatValue();
            }

            log.debug("Generated embedding for text ({} chars) → {} dims", text.length(), vector.length);
            return vector;

        } catch (Exception e) {
            log.error("OpenAI embedding API call failed: {}", e.getMessage());
            return new float[dimensions];
        }
    }

    /**
     * @return the configured embedding dimensions
     */
    public int getDimensions() {
        return dimensions;
    }

    /**
     * @return true if the OpenAI API key is configured
     */
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }
}
