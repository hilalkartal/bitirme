package com.bitirme.demo_bitirme.repository;

import com.bitirme.demo_bitirme.data.entity.PhotoTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PhotoTagRepository extends JpaRepository<PhotoTag, Long> {
}
