package com.bitirme.demo_bitirme.repository;

import com.bitirme.demo_bitirme.data.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    /** Find an existing tag by its unique composite key (name + type + source). */
    Optional<Tag> findByNameAndTagTypeAndSource(String name, Tag.TagType tagType, Tag.TagSource source);

    /** Search tags by partial name across all types — case-insensitive. */
    List<Tag> findByNameContainingIgnoreCaseOrderByTagTypeAscNameAsc(String name);
}
