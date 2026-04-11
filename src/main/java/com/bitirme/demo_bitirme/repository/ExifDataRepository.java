package com.bitirme.demo_bitirme.repository;

import com.bitirme.demo_bitirme.data.entity.ExifData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExifDataRepository extends JpaRepository<ExifData, Long> {
}
