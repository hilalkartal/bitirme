package com.bitirme.demo_bitirme.controller;

import com.bitirme.demo_bitirme.data.dto.PhotoDTO;
import com.bitirme.demo_bitirme.data.dto.UpdatePhotoRequest;
import com.bitirme.demo_bitirme.service.PhotoService;
import com.bitirme.demo_bitirme.util.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/photos")
@RequiredArgsConstructor
public class PhotoController {

    private final PhotoService photoService;

    /**
     * Upload a new photo
     * POST /photos/upload
     */
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<Long>> uploadPhoto(@RequestParam("file") MultipartFile file) {
        log.info("Received photo upload request: {}", file.getOriginalFilename());
        PhotoDTO uploadedPhoto = photoService.uploadPhoto(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Photo uploaded successfully", uploadedPhoto.getId())
        );
    }

    /**
     * Get photo by ID
     * GET /photos/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PhotoDTO>> getPhotoById(@PathVariable Long id) {
        log.info("Retrieving photo with ID: {}", id);
        PhotoDTO photo = photoService.getPhotoById(id);
        return ResponseEntity.ok(
                ApiResponse.success("Photo retrieved successfully", photo)
        );
    }

    /**
     * Get all photos with pagination
     * GET /photos?page=0&size=10
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PhotoDTO>>> getAllPhotos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Retrieving all photos - page: {}, size: {}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<PhotoDTO> photos = photoService.getAllPhotos(pageable);
        return ResponseEntity.ok(
                ApiResponse.success("Photos retrieved successfully", photos)
        );
    }

    /**
     * Update photo metadata (filename, EXIF, GPS)
     * PUT /photos/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PhotoDTO>> updatePhoto(
            @PathVariable Long id,
            @RequestBody UpdatePhotoRequest request) {
        log.info("Updating photo with ID: {}", id);
        PhotoDTO updated = photoService.updatePhoto(id, request);
        return ResponseEntity.ok(
                ApiResponse.success("Photo updated successfully", updated)
        );
    }

    /**
     * Delete a photo and its file from disk
     * DELETE /photos/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePhoto(@PathVariable Long id) {
        log.info("Deleting photo with ID: {}", id);
        photoService.deletePhoto(id);
        return ResponseEntity.ok(
                ApiResponse.success("Photo deleted successfully")
        );
    }

    /**
     * Add a manual tag to a photo
     * POST /photos/{id}/tags
     * Body: { "name": "...", "tagType": "CUSTOM" }
     */
    @PostMapping("/{id}/tags")
    public ResponseEntity<ApiResponse<PhotoDTO>> addTag(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> body) {
        String name    = body.get("name");
        String tagType = body.getOrDefault("tagType", "CUSTOM");
        String source  = body.getOrDefault("source", "MANUAL");   // SYSTEM for auto-detected tags
        log.info("Adding {} tag '{}' ({}) to photo {}", source, name, tagType, id);
        PhotoDTO updated = photoService.addTag(id, name, tagType, source);
        return ResponseEntity.ok(ApiResponse.success("Tag added", updated));
    }

    /**
     * Remove a manual tag from a photo
     * DELETE /photos/{id}/tags/{photoTagId}
     */
    @DeleteMapping("/{id}/tags/{photoTagId}")
    public ResponseEntity<ApiResponse<PhotoDTO>> removeTag(
            @PathVariable Long id,
            @PathVariable Long photoTagId) {
        log.info("Removing tag {} from photo {}", photoTagId, id);
        PhotoDTO updated = photoService.removeTag(id, photoTagId);
        return ResponseEntity.ok(ApiResponse.success("Tag removed", updated));
    }
}
