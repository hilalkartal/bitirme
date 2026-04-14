package com.bitirme.demo_bitirme.repository;

import com.bitirme.demo_bitirme.data.entity.AlbumPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlbumPhotoRepository extends JpaRepository<AlbumPhoto, Long> {

    List<AlbumPhoto> findByAlbumIdOrderByAddedAtDesc(Long albumId);

    Optional<AlbumPhoto> findByAlbumIdAndPhotoId(Long albumId, Long photoId);

    boolean existsByAlbumIdAndPhotoId(Long albumId, Long photoId);
}
