package com.bitirme.demo_bitirme.repository;

import com.bitirme.demo_bitirme.data.entity.Album;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlbumRepository extends JpaRepository<Album, Long> {

    List<Album> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    /** All albums this user can see — owned by them OR they're a collaborator on it. */
    @Query("""
           SELECT DISTINCT a FROM Album a
           LEFT JOIN a.collaborators c
           WHERE a.owner.id = :userId
              OR c.user.id = :userId
           ORDER BY a.createdAt DESC
           """)
    List<Album> findAllVisibleToUser(@Param("userId") Long userId);
}
