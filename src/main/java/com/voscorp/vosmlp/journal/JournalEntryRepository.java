package com.voscorp.vosmlp.journal;

import com.voscorp.vosmlp.students.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {
    List<JournalEntry> findByJournalColumnIn(List<JournalColumn> columns);
    Optional<JournalEntry> findByStudentAndJournalColumn(Student student, JournalColumn journalColumn);
    List<JournalEntry> findByStudentAndJournalColumnIn(Student student, List<JournalColumn> columns);
    List<JournalEntry> findByStudent(Student student);
}