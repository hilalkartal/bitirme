package com.bitirme.demo_bitirme.service;

import com.bitirme.demo_bitirme.data.dto.DetectedLocationDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Calls the Google Cloud Vision API Landmark Detection endpoint.
 *
 * Setup:
 *   1. Enable "Cloud Vision API" in Google Cloud Console.
 *   2. Create an API key (Credentials → Create Credentials → API key).
 *   3. Set  google.vision.api-key  in application.yaml.
 *
 * Pricing: First 1,000 landmark detection units/month are free.
 * The $200/month Google Cloud credit covers far more than a typical graduation project needs.
 */
@Slf4j
@Service
public class LandmarkDetectionService {

    private static final String VISION_URL =
            "https://vision.googleapis.com/v1/images:annotate?key=";

    @Value("${google.vision.api-key:}")
    private String apiKey;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Reads the image file at {@code filePath}, sends it to the Vision API,
     * and returns up to 5 detected landmark candidates ordered by confidence.
     *
     * @throws IllegalStateException if the API key is not configured
     * @throws RuntimeException      on network or API errors
     */
    public List<DetectedLocationDTO> detectLandmarks(String filePath) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                "Google Vision API key is not configured. " +
                "Set 'google.vision.api-key' in application.yaml.");
        }

        // Read and base64-encode the image
        byte[] imageBytes = Files.readAllBytes(Paths.get(filePath));
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        // Build Vision API request body
        String requestBody = MAPPER.writeValueAsString(
            java.util.Map.of("requests", java.util.List.of(
                java.util.Map.of(
                    "image",    java.util.Map.of("content", base64Image),
                    "features", java.util.List.of(
                        java.util.Map.of("type", "LANDMARK_DETECTION", "maxResults", 5)
                    )
                )
            ))
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(VISION_URL + apiKey))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        log.info("Calling Google Vision Landmark Detection for file: {}", filePath);
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("Vision API returned {}: {}", response.statusCode(), response.body());
            throw new RuntimeException("Vision API error " + response.statusCode() + ": " + response.body());
        }

        return parseLandmarks(response.body());
    }

    private List<DetectedLocationDTO> parseLandmarks(String json) throws Exception {
        List<DetectedLocationDTO> results = new ArrayList<>();

        JsonNode root = MAPPER.readTree(json);
        JsonNode annotations = root
                .path("responses").path(0)
                .path("landmarkAnnotations");

        if (annotations.isMissingNode() || !annotations.isArray()) {
            log.info("Vision API: no landmarks detected");
            return results;
        }

        for (JsonNode landmark : annotations) {
            String name  = landmark.path("description").asText();
            float  score = (float) landmark.path("score").asDouble();

            JsonNode locations = landmark.path("locations");
            if (!locations.isArray() || locations.isEmpty()) continue;

            JsonNode latLng = locations.path(0).path("latLng");
            double lat = latLng.path("latitude").asDouble();
            double lng = latLng.path("longitude").asDouble();

            results.add(new DetectedLocationDTO(name, lat, lng, score));
            log.info("Detected landmark: {} ({}) at {},{}", name, score, lat, lng);
        }

        return results;
    }
}
