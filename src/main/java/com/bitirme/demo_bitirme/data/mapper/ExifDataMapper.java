package com.bitirme.demo_bitirme.data.mapper;

import com.bitirme.demo_bitirme.data.dto.EXIFDataDTO;
import com.bitirme.demo_bitirme.data.entity.ExifData;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ExifDataMapper {

    @Mapping(target = "photo", ignore = true)
    ExifData toExifData(EXIFDataDTO dto);

    EXIFDataDTO toExifDataDTO(ExifData entity);
}
