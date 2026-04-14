package com.bitirme.demo_bitirme.data.mapper;

import com.bitirme.demo_bitirme.data.dto.AlbumDTO;
import com.bitirme.demo_bitirme.data.dto.AlbumPhotoDTO;
import com.bitirme.demo_bitirme.data.dto.UserDTO;
import com.bitirme.demo_bitirme.data.entity.Album;
import com.bitirme.demo_bitirme.data.entity.AlbumCollaborator;
import com.bitirme.demo_bitirme.data.entity.AlbumPhoto;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AlbumMapper {

    private final UserMapper userMapper;
    private final PhotoMapper photoMapper;

    public AlbumMapper(UserMapper userMapper, PhotoMapper photoMapper) {
        this.userMapper = userMapper;
        this.photoMapper = photoMapper;
    }

    public AlbumDTO toDTO(Album album) {
        if (album == null) return null;

        List<UserDTO> collabs = album.getCollaborators() == null
                ? List.of()
                : album.getCollaborators().stream()
                        .map(AlbumCollaborator::getUser)
                        .map(userMapper::toUserDTO)
                        .toList();

        int photoCount = album.getAlbumPhotos() == null ? 0 : album.getAlbumPhotos().size();

        return new AlbumDTO(
                album.getId(),
                album.getName(),
                album.getDescription(),
                album.getAlbumType().name(),
                userMapper.toUserDTO(album.getOwner()),
                collabs,
                photoCount,
                album.getCreatedAt(),
                album.getUpdatedAt()
        );
    }

    public AlbumPhotoDTO toAlbumPhotoDTO(AlbumPhoto ap) {
        if (ap == null) return null;
        return new AlbumPhotoDTO(
                ap.getId(),
                photoMapper.toPhotoDTO(ap.getPhoto()),
                userMapper.toUserDTO(ap.getAddedBy()),
                ap.getAddedAt()
        );
    }
}
