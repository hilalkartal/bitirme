package com.bitirme.demo_bitirme.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Calls the Python FastAPI ML service asynchronously after a photo is uploaded.
 * The Python service classifies the photo (people vs. scenery), detects faces,
 * matches them to known persons, and posts FACE tags back to Spring Boot.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PythonMLService {

    @Value("${app.python.api-url}")
    private String pythonApiUrl;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Notifies the Python ML service that a FACE tag has been renamed for a specific user.
     * The Python side should update its per-user label store, not the global person name.
     *
     * @param oldName the previous tag name (e.g. "Person 2")
     * @param newName the new tag name (e.g. "Mom")
     * @param userId  which user's label was renamed
     */
    @Async
    public void renamePersonForUserAsync(String oldName, String newName, Long userId) {
        try {
            String body = MAPPER.writeValueAsString(Map.of(
                    "old_name", oldName,
                    "new_name", newName,
                    "user_id", userId
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(pythonApiUrl + "/rename-person"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            log.info("Notifying Python ML service of rename for user {}: '{}' → '{}'", userId, oldName, newName);
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                log.info("Python ML service confirmed rename: {}", response.body());
            } else {
                log.warn("Python ML service returned {} for rename '{}' → '{}' (user {}): {}",
                        response.statusCode(), oldName, newName, userId, response.body());
            }

        } catch (java.net.ConnectException e) {
            log.warn("Python ML service unreachable during rename '{}' → '{}' (user {}).",
                    oldName, newName, userId);
        } catch (Exception e) {
            log.error("Failed to notify Python ML service of rename '{}' → '{}' (user {}): {}",
                    oldName, newName, userId, e.getMessage(), e);
        }
    }

    /**
     * Asynchronously sends the photo to the Python /analyze-photo endpoint.
     * Runs in a separate thread so it does not delay the upload response.
     *
     * @param photoId  Spring Boot photo ID (used by Python to post tags back)
     * @param filePath Absolute path to the saved image on disk
     * @param ownerUserId the user who owns this photo — Python posts SYSTEM tags back on their behalf
     */
    @Async
    public void analyzePhotoAsync(Long photoId, String filePath, Long ownerUserId) {
        try {
            String body = MAPPER.writeValueAsString(Map.of(
                    "photo_id", photoId,
                    "file_path", filePath,
                    "owner_user_id", ownerUserId
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(pythonApiUrl + "/analyze-photo"))
                    .timeout(Duration.ofSeconds(120))  // ML inference can be slow
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            log.info("Sending photo {} to Python ML service for analysis", photoId);
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                log.info("ML analysis complete for photo {}: {}", photoId, response.body());
            } else {
                log.warn("Python ML service returned {} for photo {}: {}",
                        response.statusCode(), photoId, response.body());
            }

        } catch (java.net.ConnectException e) {
            log.warn("Python ML service is not reachable (photo {}). " +
                     "Start the FastAPI server on {} to enable auto-tagging.", photoId, pythonApiUrl);
        } catch (Exception e) {
            log.error("Failed to analyze photo {} with Python ML service: {}", photoId, e.getMessage(), e);
        }
    }
}
