package com.bitirme.demo_bitirme.repository;

import com.bitirme.demo_bitirme.data.entity.PhotoTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PhotoTagRepository extends JpaRepository<PhotoTag, Long> {

    /** Prevent adding the same tag twice to the same photo. */
    Optional<PhotoTag> findByPhotoIdAndTagId(Long photoId, Long tagId);
}
