package com.bitirme.demo_bitirme.data.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PhotoDTO {
    private Long id;
    private String fileName;
    private String filePath;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private long size;

    private EXIFDataDTO exifData;
    private GPSDataDTO gpsData;
    private List<PhotoTagDTO> photoTags;
}
