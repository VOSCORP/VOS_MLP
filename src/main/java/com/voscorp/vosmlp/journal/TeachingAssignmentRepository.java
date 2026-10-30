package com.voscorp.vosmlp.journal;

import com.voscorp.vosmlp.auth.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TeachingAssignmentRepository extends JpaRepository<TeachingAssignment, Long> {
    List<TeachingAssignment> findByTeacher(AppUser teacher);
    List<TeachingAssignment> findByTeacherAndStudyGroup(AppUser teacher, StudyGroup studyGroup);
    boolean existsByTeacherAndStudyGroupAndSubject(AppUser teacher, StudyGroup studyGroup, Subject subject);
}