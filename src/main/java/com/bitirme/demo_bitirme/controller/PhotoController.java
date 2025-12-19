package com.bitirme.demo_bitirme.controller;

import com.bitirme.demo_bitirme.data.Photo;
import com.bitirme.demo_bitirme.service.PhotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

@RestController
@RequestMapping("/photos")
@RequiredArgsConstructor
public class PhotoController {

    private final PhotoService photoService;

    @PostMapping(value = "/new-photo", consumes = "multipart/form-data")
    public ResponseEntity<Photo> addPhoto(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description
    ) {
        Photo savedPhoto = photoService.savePhoto(file, description);
        Path savedPhotoPath = Path.of("uploads").resolve(savedPhoto.getFileName());
        return ResponseEntity.ok(savedPhoto);
    }
}
