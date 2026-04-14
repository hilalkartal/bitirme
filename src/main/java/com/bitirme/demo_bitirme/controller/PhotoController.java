package com.bitirme.demo_bitirme.controller;

import com.bitirme.demo_bitirme.config.CurrentUserId;
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

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<Long>> uploadPhoto(@RequestParam("file") MultipartFile file,
                                                         @CurrentUserId Long userId) {
        log.info("Received photo upload request: {} (user={})", file.getOriginalFilename(), userId);
        PhotoDTO uploadedPhoto = photoService.uploadPhoto(file, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Photo uploaded successfully", uploadedPhoto.getId())
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PhotoDTO>> getPhotoById(@PathVariable Long id,
                                                              @CurrentUserId Long userId) {
        PhotoDTO photo = photoService.getPhotoById(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Photo retrieved successfully", photo));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PhotoDTO>>> getAllPhotos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @CurrentUserId Long userId) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PhotoDTO> photos = photoService.getAllPhotos(pageable, userId);
        return ResponseEntity.ok(ApiResponse.success("Photos retrieved successfully", photos));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PhotoDTO>> updatePhoto(
            @PathVariable Long id,
            @RequestBody UpdatePhotoRequest request,
            @CurrentUserId Long userId) {
        PhotoDTO updated = photoService.updatePhoto(id, request, userId);
        return ResponseEntity.ok(ApiResponse.success("Photo updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePhoto(@PathVariable Long id,
                                                         @CurrentUserId Long userId) {
        photoService.deletePhoto(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Photo deleted successfully"));
    }

    @PostMapping("/{id}/tags")
    public ResponseEntity<ApiResponse<PhotoDTO>> addTag(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> body,
            @CurrentUserId Long userId) {
        String name    = body.get("name");
        String tagType = body.getOrDefault("tagType", "CUSTOM");
        String source  = body.getOrDefault("source", "MANUAL");
        PhotoDTO updated = photoService.addTag(id, name, tagType, source, userId);
        return ResponseEntity.ok(ApiResponse.success("Tag added", updated));
    }

    /**
     * Copy a photo into the caller's own gallery.
     * The file is shared; only DB rows are duplicated.
     * Any tags the caller already added to the source photo travel to the copy.
     */
    @PostMapping("/{id}/copy")
    public ResponseEntity<ApiResponse<PhotoDTO>> copyPhoto(
            @PathVariable Long id,
            @CurrentUserId Long userId) {
        PhotoDTO copy = photoService.copyPhoto(id, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Photo copied to your gallery", copy));
    }

    @DeleteMapping("/{id}/tags/{photoTagId}")
    public ResponseEntity<ApiResponse<PhotoDTO>> removeTag(
            @PathVariable Long id,
            @PathVariable Long photoTagId,
            @CurrentUserId Long userId) {
        PhotoDTO updated = photoService.removeTag(id, photoTagId, userId);
        return ResponseEntity.ok(ApiResponse.success("Tag removed", updated));
    }
}
