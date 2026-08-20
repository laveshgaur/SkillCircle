package com.skillcircle.matching.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;

/**
 * Client for Qdrant vector database HTTP API.
 * Handles collection management, vector upsert, search (ANN), and deletion.
 *
 * Falls back gracefully when Qdrant is not running.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorDBClient {

    @Value("${app.qdrant.url:http://localhost:6333}")
    private String qdrantUrl;

    @Value("${app.qdrant.collection:skillcircle_profiles}")
    private String collectionName;

    @Value("${app.openai.embedding-dimensions:1536}")
    private int dimensions;

    private RestClient restClient;

    private RestClient getClient() {
        if (restClient == null) {
            restClient = RestClient.builder()
                    .baseUrl(qdrantUrl)
                    .defaultHeader("Content-Type", "application/json")
                    .build();
        }
        return restClient;
    }

    /**
     * Ensure the Qdrant collection exists. Creates it if not.
     */
    public void ensureCollection() {
        try {
            getClient().get()
                    .uri("/collections/{name}", collectionName)
                    .retrieve()
                    .body(Map.class);
            log.debug("Qdrant collection '{}' already exists", collectionName);
        } catch (Exception e) {
            // Collection doesn't exist — create it
            try {
                Map<String, Object> body = Map.of(
                        "vectors", Map.of(
                                "size", dimensions,
                                "distance", "Cosine"
                        )
                );
                getClient().put()
                        .uri("/collections/{name}", collectionName)
                        .body(body)
                        .retrieve()
                        .body(Map.class);
                log.info("Created Qdrant collection '{}' (dims={}, cosine)", collectionName, dimensions);
            } catch (Exception ex) {
                log.warn("Failed to create Qdrant collection: {}", ex.getMessage());
            }
        }
    }

    /**
     * Upsert a vector with payload into the collection.
     *
     * @param pointId unique ID (user UUID as string)
     * @param vector  the embedding vector
     * @param payload metadata (goalType, experienceLevel, timezone, availability)
     */
    @SuppressWarnings("unchecked")
    public void upsert(String pointId, float[] vector, Map<String, Object> payload) {
        try {
            List<Double> vectorList = new ArrayList<>(vector.length);
            for (float v : vector) {
                vectorList.add((double) v);
            }

            Map<String, Object> point = Map.of(
                    "id", pointId,
                    "vector", vectorList,
                    "payload", payload
            );

            Map<String, Object> body = Map.of("points", List.of(point));

            getClient().put()
                    .uri("/collections/{name}/points", collectionName)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            log.debug("Upserted vector for point: {}", pointId);
        } catch (Exception e) {
            log.error("Qdrant upsert failed for point {}: {}", pointId, e.getMessage());
        }
    }

    /**
     * Approximate Nearest Neighbor search.
     *
     * @param queryVector the search vector
     * @param topK        max number of results
     * @param excludeId   point ID to exclude (self)
     * @return list of (pointId, score) pairs
     */
    @SuppressWarnings("unchecked")
    public List<VectorMatch> searchANN(float[] queryVector, int topK, String excludeId) {
        try {
            List<Double> vectorList = new ArrayList<>(queryVector.length);
            for (float v : queryVector) {
                vectorList.add((double) v);
            }

            Map<String, Object> filter = Map.of(
                    "must_not", List.of(
                            Map.of("has_id", List.of(excludeId))
                    )
            );

            Map<String, Object> body = Map.of(
                    "vector", vectorList,
                    "limit", topK,
                    "filter", filter,
                    "with_payload", true
            );

            Map<String, Object> response = getClient().post()
                    .uri("/collections/{name}/points/search", collectionName)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("result")) {
                return Collections.emptyList();
            }

            List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("result");
            List<VectorMatch> matches = new ArrayList<>();

            for (Map<String, Object> result : results) {
                String id = String.valueOf(result.get("id"));
                double score = ((Number) result.get("score")).doubleValue();
                Map<String, Object> payload = result.containsKey("payload")
                        ? (Map<String, Object>) result.get("payload")
                        : Collections.emptyMap();
                matches.add(new VectorMatch(id, score, payload));
            }

            log.debug("ANN search returned {} results (topK={})", matches.size(), topK);
            return matches;

        } catch (Exception e) {
            log.error("Qdrant ANN search failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Delete a vector point by ID.
     */
    public void delete(String pointId) {
        try {
            Map<String, Object> body = Map.of("points", List.of(pointId));
            getClient().post()
                    .uri("/collections/{name}/points/delete", collectionName)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            log.debug("Deleted vector point: {}", pointId);
        } catch (Exception e) {
            log.warn("Qdrant delete failed for point {}: {}", pointId, e.getMessage());
        }
    }

    /**
     * Check if Qdrant is reachable.
     */
    public boolean isAvailable() {
        try {
            getClient().get().uri("/").retrieve().body(Map.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * A vector search result with ID, similarity score, and payload.
     */
    public record VectorMatch(String id, double score, Map<String, Object> payload) {}
}
