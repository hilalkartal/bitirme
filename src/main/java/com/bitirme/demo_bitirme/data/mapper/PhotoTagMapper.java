package com.bitirme.demo_bitirme.data.mapper;

import com.bitirme.demo_bitirme.data.dto.PhotoTagDTO;
import com.bitirme.demo_bitirme.data.entity.PhotoTag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = TagMapper.class)
public interface PhotoTagMapper {

    @Mapping(target = "photo", ignore = true)
    @Mapping(target = "addedBy", ignore = true)
    @Mapping(target = "tag", source = "tag")
    PhotoTag toPhotoTag(PhotoTagDTO dto);

    @Mapping(target = "tag", source = "tag")
    PhotoTagDTO toPhotoTagDTO(PhotoTag entity);
}
