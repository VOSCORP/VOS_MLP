package com.voscorp.vosmlp.discipline;

import com.voscorp.vosmlp.documents.AbsenceApplication;
import com.voscorp.vosmlp.students.Student;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class DisciplinaryNotice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Student student;

    @ManyToOne
    private AbsenceApplication application;

    private String reason;
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private NoticeStatus status;

    protected DisciplinaryNotice() {}

    public DisciplinaryNotice(Student student, AbsenceApplication application, String reason) {
        this.student = student;
        this.application = application;
        this.reason = reason;
        this.createdAt = LocalDateTime.now();
        this.status = NoticeStatus.ACTIVE;
    }

    public Long getId() { return id; }
    public Student getStudent() { return student; }
    public AbsenceApplication getApplication() { return application; }
    public String getReason() { return reason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public NoticeStatus getStatus() { return status; }
}