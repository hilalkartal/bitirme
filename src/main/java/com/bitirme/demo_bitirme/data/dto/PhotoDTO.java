package com.bitirme.demo_bitirme.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhotoDTO {
    private String fileName;
    private String description;
    private String url;
    private long size;         // dosya boyutu
    private String contentType; // image/jpeg, image/png ?
}
