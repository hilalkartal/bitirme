package com.bitirme.demo_bitirme.exception;

import com.bitirme.demo_bitirme.util.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PhotoUploadException.class)
    public ResponseEntity<ApiResponse<?>> handlePhotoUploadException(PhotoUploadException ex) {
        log.error("Photo upload error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ApiResponse<?>> handleFileStorageException(FileStorageException ex) {
        log.error("File storage error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to store file: " + ex.getMessage()));
    }

    @ExceptionHandler(PhotoNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handlePhotoNotFoundException(PhotoNotFoundException ex) {
        log.error("Photo not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ExifExtractionException.class)
    public ResponseEntity<ApiResponse<?>> handleExifExtractionException(ExifExtractionException ex) {
        log.warn("EXIF extraction error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.error("Could not extract EXIF data: " + ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred"));
    }
}
