package com.bitirme.demo_bitirme.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final UploadConfig uploadConfig;

    /**
     * Serve uploaded photos as static files.
     * GET /uploads/{filename}  →  app.upload.dir/{filename}
     *
     * With the context-path /bitirme the full URL becomes:
     *   http://localhost:8081/bitirme/uploads/{filename}
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadDir = uploadConfig.getUploadPath().toAbsolutePath();
        String resourceLocation = "file:" + uploadDir + "/";

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(resourceLocation);
    }
}
