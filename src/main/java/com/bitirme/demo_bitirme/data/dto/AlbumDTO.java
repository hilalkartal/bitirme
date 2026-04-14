package com.bitirme.demo_bitirme.data.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlbumDTO {
    private Long id;
    private String name;
    private String description;
    private String albumType;            // PRIVATE / COLLABORATIVE
    private UserDTO owner;
    private List<UserDTO> collaborators; // may be empty
    private int photoCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
