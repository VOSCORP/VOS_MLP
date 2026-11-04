package com.voscorp.vosmlp.messages;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LetterRepository extends JpaRepository<Letter, Long> {
    List<Letter> findByThreadOrderByCreatedAtAsc(LetterThread thread);
}