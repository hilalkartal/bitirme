package com.bitirme.demo_bitirme.repository;

import com.bitirme.demo_bitirme.data.entity.Photo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, Long> {

}
