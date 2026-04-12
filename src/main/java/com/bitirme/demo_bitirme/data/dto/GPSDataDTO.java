package com.bitirme.demo_bitirme.data.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GPSDataDTO {
    private Long id;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal altitude;
    private String country;
    private String city;
    private String googleMapsLink;
}



