package com.bitirme.demo_bitirme.service;

import com.bitirme.demo_bitirme.data.dto.PhotoDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
@RequiredArgsConstructor
public class PhotoService {

    public PhotoDTO savePhoto(MultipartFile file, String description) {
        try {
            // if uploads folder exists use it
            // else create uploads folder
            Path uploadDir = Paths.get("uploads/");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            // save photo
            Path filePath = uploadDir.resolve(file.getOriginalFilename());
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            ///  burada
            // create dto to return
            PhotoDTO dto = new PhotoDTO();
            dto.setFileName(file.getOriginalFilename());
            dto.setDescription(description);
            dto.setUrl("/uploads/" + file.getOriginalFilename()); // frontend'de göstermek için

            return dto;

        } catch (IOException e) {
            throw new RuntimeException("Fotoğraf yüklenemedi", e);
        }
    }
}

