package com.voscorp.vosmlp.journal;

import com.voscorp.vosmlp.auth.AppUser;
import jakarta.persistence.*;

@Entity
public class TeachingAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private AppUser teacher;

    @ManyToOne
    private StudyGroup studyGroup;

    @ManyToOne
    private Subject subject;

    protected TeachingAssignment() {}

    public TeachingAssignment(AppUser teacher, StudyGroup studyGroup, Subject subject) {
        this.teacher = teacher;
        this.studyGroup = studyGroup;
        this.subject = subject;
    }

    public Long getId() { return id; }
    public AppUser getTeacher() { return teacher; }
    public StudyGroup getStudyGroup() { return studyGroup; }
    public Subject getSubject() { return subject; }
}