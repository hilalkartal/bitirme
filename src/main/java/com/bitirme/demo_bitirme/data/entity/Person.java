package com.bitirme.demo_bitirme.data.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A face cluster identity. The display_name column is an internal/default
 * name (e.g. "Person 2" from the Python ML service) — per-user display names
 * live in {@link PersonUserLabel}.
 */
@Entity
@Table(name = "persons")
@Setter
@Getter
@NoArgsConstructor
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    private Long id;

    /** Internal fallback name, assigned by the Python ML service. Not shown to users directly. */
    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Lob
    @Column(name = "centroid_embedding")
    private byte[] centroidEmbedding;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
