package com.bitirme.demo_bitirme.data.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * A photo inside an album, including who added it (so the UI can only show
 * the delete button to that user).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlbumPhotoDTO {
    private Long id;           // album_photo.id (used for removing from album)
    private PhotoDTO photo;
    private UserDTO addedBy;
    private LocalDateTime addedAt;
}
