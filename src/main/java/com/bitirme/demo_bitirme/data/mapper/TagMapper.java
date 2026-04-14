package com.bitirme.demo_bitirme.data.mapper;

import com.bitirme.demo_bitirme.data.dto.TagDTO;
import com.bitirme.demo_bitirme.data.entity.Tag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TagMapper {

    @Mapping(target = "user", ignore = true)
    Tag toTag(TagDTO dto);

    @Mapping(target = "userId", source = "user.id")
    TagDTO toTagDTO(Tag entity);
}
