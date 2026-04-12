package com.bitirme.demo_bitirme.data.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "exif_data")
@Setter
@Getter
@NoArgsConstructor
public class ExifData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    private Long id;

    @OneToOne
    @JoinColumn(name = "photo_id", nullable = false)
    private Photo photo;

    @Column(name = "camera_make")
    private String cameraMake;

    @Column(name = "camera_model")
    private String cameraModel;

    @Column(name = "iso")
    private Integer iso;

    @Column(name = "aperture")
    private BigDecimal aperture;

    @Column(name = "exposure_time")
    private String exposureTime;

    @Column(name = "focal_length")
    private BigDecimal focalLength;

    @Column(name = "date_taken")
    private LocalDateTime dateTaken;

    @Column(name = "orientation")
    private Integer orientation;
}
