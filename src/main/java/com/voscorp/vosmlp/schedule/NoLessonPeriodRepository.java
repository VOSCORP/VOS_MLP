package com.voscorp.vosmlp.schedule;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoLessonPeriodRepository extends JpaRepository<NoLessonPeriod, Long> {
    List<NoLessonPeriod> findBySemester(Semester semester);
}