package com.voscorp.vosmlp.journal;

import com.voscorp.vosmlp.auth.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JournalGrantRepository extends JpaRepository<JournalGrant, Long> {
    List<JournalGrant> findByStarosta(AppUser starosta);
    List<JournalGrant> findByTeacherOrderByCreatedAtDesc(AppUser teacher);
    List<JournalGrant> findByStatusIn(List<GrantStatus> statuses);
}