package com.voscorp.vosmlp.attendance;

import com.voscorp.vosmlp.journal.JournalColumn;
import com.voscorp.vosmlp.students.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
    List<AttendanceRecord> findByJournalColumnIn(List<JournalColumn> columns);
    Optional<AttendanceRecord> findByStudentAndJournalColumn(Student student, JournalColumn journalColumn);
    List<AttendanceRecord> findByStudentAndJournalColumnIn(Student student, List<JournalColumn> columns);
}