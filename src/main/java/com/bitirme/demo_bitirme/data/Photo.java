package com.bitirme.demo_bitirme.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Photo {
    private Long id;
    private User userId;
    private String fileName;
    private String description;
    private String url;
    private long size;         // dosya boyutu
    private String contentType; // image/jpeg, image/png ?

    private EXIFData exifData;
}
