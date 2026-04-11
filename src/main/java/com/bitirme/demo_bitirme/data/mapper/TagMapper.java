package com.bitirme.demo_bitirme.data.mapper;

import com.bitirme.demo_bitirme.data.dto.TagDTO;
import com.bitirme.demo_bitirme.data.entity.Tag;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TagMapper {

    Tag toTag(TagDTO dto);

    TagDTO toTagDTO(Tag entity);
}
