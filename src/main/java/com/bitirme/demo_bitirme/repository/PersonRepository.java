package com.bitirme.demo_bitirme.repository;

import com.bitirme.demo_bitirme.data.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PersonRepository extends JpaRepository<Person, Long> {
    Optional<Person> findByDisplayName(String displayName);
}
