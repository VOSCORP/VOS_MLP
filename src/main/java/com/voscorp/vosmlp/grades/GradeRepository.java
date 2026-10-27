package com.voscorp.vosmlp.grades;

import com.voscorp.vosmlp.students.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GradeRepository extends JpaRepository<Grade, Long> {
    List<Grade> findByStudent(Student student);
}