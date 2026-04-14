package com.bitirme.demo_bitirme.data.mapper;

import com.bitirme.demo_bitirme.data.dto.PhotoDTO;
import com.bitirme.demo_bitirme.data.entity.Photo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {ExifDataMapper.class, GpsDataMapper.class, PhotoTagMapper.class})
public interface PhotoMapper {

    @Mapping(target = "exifData", source = "exifData")
    @Mapping(target = "gpsData", source = "gpsData")
    @Mapping(target = "photoTags", source = "photoTags")
    @Mapping(target = "owner", ignore = true)
    Photo toPhoto(PhotoDTO dto);

    @Mapping(target = "exifData", source = "exifData")
    @Mapping(target = "gpsData", source = "gpsData")
    @Mapping(target = "photoTags", source = "photoTags")
    @Mapping(target = "ownerUserId", source = "owner.id")
    PhotoDTO toPhotoDTO(Photo entity);
}
