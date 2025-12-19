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
public class GPSData {
    private double latitude;
    private double longitude;
    private Double altitude;
    private LocalDateTime timestamp;
    private String googleMapsLink;
    private GPSAddress address;

}


