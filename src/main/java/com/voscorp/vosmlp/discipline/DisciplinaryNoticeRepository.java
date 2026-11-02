package com.voscorp.vosmlp.discipline;

import com.voscorp.vosmlp.students.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DisciplinaryNoticeRepository extends JpaRepository<DisciplinaryNotice, Long> {
    long countByStudent(Student student);
    boolean existsByApplicationId(Long applicationId);
}