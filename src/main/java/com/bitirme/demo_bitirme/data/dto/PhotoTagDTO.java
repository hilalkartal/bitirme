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
public class PhotoTagDTO {
    private Long id;
    private TagDTO tag;
    private BigDecimal confidenceScore;
}
