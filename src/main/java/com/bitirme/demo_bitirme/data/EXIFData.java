package com.bitirme.demo_bitirme.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EXIFData {
    private String make;
    private String model;
    private String software;
    private LocalDateTime dateTaken;
    private int orientation;
    private double exposureTime;
    private double aperture;
    private int iso;
    private double focalLength;
    private boolean flashUsed;
    private GPSData gpsData;
}
