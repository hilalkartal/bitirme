package com.bitirme.demo_bitirme.data.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request body for PUT /photos/{id}
 * All fields are optional — null means "leave unchanged".
 */
@Data
public class UpdatePhotoRequest {

    /** Rename the photo */
    private String fileName;

    /** EXIF fields — send null to leave untouched, send the object to overwrite */
    private ExifUpdate exifData;

    /**
     * GPS fields — send null to leave untouched.
     * latitude + longitude are the only required fields when updating GPS;
     * the backend will re-run reverse geocoding to fill country/city/googleMapsLink.
     */
    private GpsUpdate gpsData;

    @Data
    public static class ExifUpdate {
        private String cameraMake;
        private String cameraModel;
        private Integer iso;
        private BigDecimal aperture;
        private String exposureTime;
        private BigDecimal focalLength;
        private LocalDateTime dateTaken;
        private Integer orientation;
    }

    @Data
    public static class GpsUpdate {
        private BigDecimal latitude;
        private BigDecimal longitude;
        private BigDecimal altitude;
    }
}
