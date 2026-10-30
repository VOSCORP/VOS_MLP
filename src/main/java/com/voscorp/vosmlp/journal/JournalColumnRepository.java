package com.voscorp.vosmlp.journal;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JournalColumnRepository extends JpaRepository<JournalColumn, Long> {
    List<JournalColumn> findByStudyGroupAndSubjectOrderByColumnDateAsc(StudyGroup studyGroup, Subject subject);
    List<JournalColumn> findByStudyGroup(StudyGroup studyGroup);
}