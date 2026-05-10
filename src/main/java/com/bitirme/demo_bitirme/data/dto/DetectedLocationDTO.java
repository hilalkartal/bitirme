package com.bitirme.demo_bitirme.data.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single location candidate returned by the
 * Google Cloud Vision Landmark Detection API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetectedLocationDTO {

    /** Human-readable landmark name, e.g. "Eiffel Tower" */
    private String name;

    /** Latitude of the detected landmark */
    private double latitude;

    /** Longitude of the detected landmark */
    private double longitude;

    /** Confidence score from Vision API (0.0 – 1.0) */
    private float score;
}
