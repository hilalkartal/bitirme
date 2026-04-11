package com.bitirme.demo_bitirme.data.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "tag", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"name", "tag_type", "source"})
})
@Setter
@Getter
@NoArgsConstructor
public class Tag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "tag_type", nullable = false)
    private TagType tagType;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private TagSource source;

    @OneToMany(mappedBy = "tag", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PhotoTag> photoTags;

    public enum TagType {
        FACE, PLACE, CAMERA, CUSTOM
    }

    public enum TagSource {
        SYSTEM, MANUAL
    }
}
