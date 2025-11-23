package com.bitirme.demo_bitirme.controller;

import com.bitirme.demo_bitirme.data.dto.PhotoDTO;
import com.bitirme.demo_bitirme.service.PhotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/photos")
@RequiredArgsConstructor
public class PhotoController {

    private final PhotoService photoService;

    @PostMapping(value = "/new-photo", consumes = "multipart/form-data")
    public ResponseEntity<PhotoDTO> addPhoto(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description
    ) {
        PhotoDTO savedPhoto = photoService.savePhoto(file, description);
        return ResponseEntity.ok(savedPhoto);
    }
}
