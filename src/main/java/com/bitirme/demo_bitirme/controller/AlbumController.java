package com.bitirme.demo_bitirme.controller;

import com.bitirme.demo_bitirme.config.CurrentUserId;
import com.bitirme.demo_bitirme.data.dto.AlbumDTO;
import com.bitirme.demo_bitirme.data.dto.AlbumPhotoDTO;
import com.bitirme.demo_bitirme.service.AlbumService;
import com.bitirme.demo_bitirme.util.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/albums")
@RequiredArgsConstructor
public class AlbumController {

    private final AlbumService albumService;

    // ─── Album CRUD ──────────────────────────────────────────

    @PostMapping
    public ResponseEntity<ApiResponse<AlbumDTO>> createAlbum(
            @RequestBody Map<String, String> body,
            @CurrentUserId Long userId) {
        AlbumDTO dto = albumService.createAlbum(
                body.get("name"),
                body.get("description"),
                body.getOrDefault("albumType", "PRIVATE"),
                userId
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Album created", dto));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AlbumDTO>>> listAlbums(@CurrentUserId Long userId) {
        return ResponseEntity.ok(ApiResponse.success("Albums", albumService.listVisibleAlbums(userId)));
    }

    @GetMapping("/{albumId}")
    public ResponseEntity<ApiResponse<AlbumDTO>> getAlbum(@PathVariable Long albumId,
                                                          @CurrentUserId Long userId) {
        return ResponseEntity.ok(ApiResponse.success("Album", albumService.getAlbum(albumId, userId)));
    }

    @PutMapping("/{albumId}")
    public ResponseEntity<ApiResponse<AlbumDTO>> renameAlbum(@PathVariable Long albumId,
                                                             @RequestBody Map<String, String> body,
                                                             @CurrentUserId Long userId) {
        return ResponseEntity.ok(ApiResponse.success("Album updated",
                albumService.renameAlbum(albumId, body.get("name"), body.get("description"), userId)));
    }

    @DeleteMapping("/{albumId}")
    public ResponseEntity<ApiResponse<Void>> deleteAlbum(@PathVariable Long albumId,
                                                         @CurrentUserId Long userId) {
        albumService.deleteAlbum(albumId, userId);
        return ResponseEntity.ok(ApiResponse.success("Album deleted"));
    }

    // ─── Collaborators ───────────────────────────────────────

    @PostMapping("/{albumId}/collaborators")
    public ResponseEntity<ApiResponse<AlbumDTO>> addCollaborator(@PathVariable Long albumId,
                                                                 @RequestBody Map<String, String> body,
                                                                 @CurrentUserId Long userId) {
        AlbumDTO dto = albumService.addCollaborator(albumId, body.get("username"), userId);
        return ResponseEntity.ok(ApiResponse.success("Collaborator added", dto));
    }

    @DeleteMapping("/{albumId}/collaborators/{collaboratorUserId}")
    public ResponseEntity<ApiResponse<Void>> removeCollaborator(@PathVariable Long albumId,
                                                                @PathVariable Long collaboratorUserId,
                                                                @CurrentUserId Long userId) {
        albumService.removeCollaborator(albumId, collaboratorUserId, userId);
        return ResponseEntity.ok(ApiResponse.success("Collaborator removed"));
    }

    // ─── Album photos ────────────────────────────────────────

    @GetMapping("/{albumId}/photos")
    public ResponseEntity<ApiResponse<List<AlbumPhotoDTO>>> listPhotos(@PathVariable Long albumId,
                                                                       @CurrentUserId Long userId) {
        return ResponseEntity.ok(ApiResponse.success("Photos",
                albumService.listAlbumPhotos(albumId, userId)));
    }

    @PostMapping("/{albumId}/photos")
    public ResponseEntity<ApiResponse<AlbumPhotoDTO>> addPhoto(@PathVariable Long albumId,
                                                               @RequestBody Map<String, Long> body,
                                                               @CurrentUserId Long userId) {
        Long photoId = body.get("photoId");
        return ResponseEntity.ok(ApiResponse.success("Photo added",
                albumService.addPhotoToAlbum(albumId, photoId, userId)));
    }

    @DeleteMapping("/{albumId}/photos/{albumPhotoId}")
    public ResponseEntity<ApiResponse<Void>> removePhoto(@PathVariable Long albumId,
                                                         @PathVariable Long albumPhotoId,
                                                         @CurrentUserId Long userId) {
        albumService.removePhotoFromAlbum(albumId, albumPhotoId, userId);
        return ResponseEntity.ok(ApiResponse.success("Photo removed"));
    }
}
