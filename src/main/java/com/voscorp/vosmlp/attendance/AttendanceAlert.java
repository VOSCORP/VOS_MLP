package com.voscorp.vosmlp.attendance;

import com.voscorp.vosmlp.students.Student;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

// Запись о сработавшем пороге (одна на правило, студента, предмет и период)
@Entity
public class AttendanceAlert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_rule")
    private AlertRule rule;

    private Long subjectKey;
    private LocalDate periodStart;

    @Column(length = 400)
    private String details;

    private LocalDateTime createdAt;

    protected AttendanceAlert() {}

    public AttendanceAlert(Student student, AlertRule rule, Long subjectKey, LocalDate periodStart, String details) {
        this.student = student;
        this.rule = rule;
        this.subjectKey = subjectKey;
        this.periodStart = periodStart;
        this.details = details;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Student getStudent() { return student; }
    public AlertRule getRule() { return rule; }
    public Long getSubjectKey() { return subjectKey; }
    public LocalDate getPeriodStart() { return periodStart; }
    public String getDetails() { return details; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public String getCreatedAtText() {
        return createdAt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }
}