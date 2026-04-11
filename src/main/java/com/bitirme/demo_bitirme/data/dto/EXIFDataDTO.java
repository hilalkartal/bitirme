package com.bitirme.demo_bitirme.data.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EXIFDataDTO {
    private Long id;
    private String cameraMake;
    private String cameraModel;
    private Integer iso;
    private BigDecimal aperture;
    private String exposureTime;
    private BigDecimal focalLength;
    private LocalDateTime dateTaken;
    private Integer orientation;
}
