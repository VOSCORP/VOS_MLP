package com.voscorp.vosmlp.journal;

import com.voscorp.vosmlp.auth.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StudyGroupRepository extends JpaRepository<StudyGroup, Long> {
    List<StudyGroup> findByCurator(AppUser curator);
}