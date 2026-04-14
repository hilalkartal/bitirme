package com.bitirme.demo_bitirme.repository;

import com.bitirme.demo_bitirme.data.entity.PersonUserLabel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PersonUserLabelRepository extends JpaRepository<PersonUserLabel, Long> {

    Optional<PersonUserLabel> findByPersonIdAndUserId(Long personId, Long userId);
}
