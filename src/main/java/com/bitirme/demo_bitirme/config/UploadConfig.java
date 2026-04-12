package com.bitirme.demo_bitirme.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@Getter
public class UploadConfig {

    @Value("${app.upload.dir}")
    private String uploadDir;

    public void ensureUploadDirExists() {
        try {
            Path path = Paths.get(uploadDir);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create upload directory: " + uploadDir, e);
        }
    }

    public Path getUploadPath() {
        return Paths.get(uploadDir);
    }
}
