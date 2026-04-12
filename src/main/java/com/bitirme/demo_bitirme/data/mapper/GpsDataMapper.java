package com.bitirme.demo_bitirme.data.mapper;

import com.bitirme.demo_bitirme.data.dto.GPSDataDTO;
import com.bitirme.demo_bitirme.data.entity.GpsData;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface GpsDataMapper {

    @Mapping(target = "photo", ignore = true)
    GpsData toGpsData(GPSDataDTO dto);

    GPSDataDTO toGpsDataDTO(GpsData entity);
}
