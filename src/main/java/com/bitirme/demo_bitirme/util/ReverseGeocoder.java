package com.bitirme.demo_bitirme.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Slf4j
@Component
public class ReverseGeocoder {

    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/reverse";
    private static final String USER_AGENT = "bitirme-photo-app/1.0 (hilal13kartal@gmail.com)";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generateGoogleMapsLink(BigDecimal latitude, BigDecimal longitude) {
        return "https://www.google.com/maps/search/?api=1&query=" +
                latitude + "," + longitude;
    }

    public String[] reverseGeocode(BigDecimal latitude, BigDecimal longitude) {
        try {
            String url = NOMINATIM_URL + "?format=jsonv2&lat=" + latitude + "&lon=" + longitude;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", USER_AGENT)
                    .GET()
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("Nominatim API returned status {}", response.statusCode());
                return new String[]{null, null};
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode addressNode = root.get("address");

            if (addressNode == null || addressNode.isNull()) {
                log.debug("No address data received from Nominatim");
                return new String[]{null, null};
            }

            String country = addressNode.has("country") ? addressNode.get("country").asText() : null;
            String city = addressNode.has("city") ? addressNode.get("city").asText() : 
                         (addressNode.has("town") ? addressNode.get("town").asText() :
                         (addressNode.has("village") ? addressNode.get("village").asText() : null));

            log.debug("Reverse geocoded coordinates: country={}, city={}", country, city);
            return new String[]{country, city};

        } catch (Exception e) {
            log.warn("Failed to reverse geocode coordinates - lat: {}, lon: {}", latitude, longitude, e);
            return new String[]{null, null};
        }
    }
}
