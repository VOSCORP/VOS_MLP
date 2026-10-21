package com.voscorp.vosmlp;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DisciplinaryNoticeRepository extends JpaRepository<DisciplinaryNotice, Long> {
    long countByStudent(Student student);
    boolean existsByApplication(AbsenceApplication application);
}