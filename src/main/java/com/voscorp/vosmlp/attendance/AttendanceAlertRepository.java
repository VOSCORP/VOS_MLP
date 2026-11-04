package com.voscorp.vosmlp.attendance;

import com.voscorp.vosmlp.students.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceAlertRepository extends JpaRepository<AttendanceAlert, Long> {
    boolean existsByStudentAndRuleAndSubjectKeyAndPeriodStart(Student student, AlertRule rule,
                                                              Long subjectKey, LocalDate periodStart);
    List<AttendanceAlert> findTop50ByOrderByCreatedAtDesc();
}