package com.bitirme.demo_bitirme.repository;

import com.bitirme.demo_bitirme.data.entity.AlbumCollaborator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlbumCollaboratorRepository extends JpaRepository<AlbumCollaborator, Long> {

    Optional<AlbumCollaborator> findByAlbumIdAndUserId(Long albumId, Long userId);

    boolean existsByAlbumIdAndUserId(Long albumId, Long userId);

    List<AlbumCollaborator> findByAlbumId(Long albumId);
}
