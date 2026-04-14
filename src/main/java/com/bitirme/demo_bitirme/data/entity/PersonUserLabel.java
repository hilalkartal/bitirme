package com.bitirme.demo_bitirme.data.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A user's personal label for a Person (face cluster).
 * User A may call Person #7 "Mom"; User B may call the same Person "Auntie".
 */
@Entity
@Table(name = "person_user_label", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"person_id", "user_id"})
})
@Setter
@Getter
@NoArgsConstructor
public class PersonUserLabel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
