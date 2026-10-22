package com.voscorp.vosmlp.attendance;

import com.voscorp.vosmlp.students.Student;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class Attendance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Student student;

    private String subject;
    private LocalDate lessonDate;

    @Enumerated(EnumType.STRING)
    private AttendanceStatus status;

    protected Attendance() {}

    public Attendance(Student student, String subject, LocalDate lessonDate, AttendanceStatus status) {
        this.student = student;
        this.subject = subject;
        this.lessonDate = lessonDate;
        this.status = status;
    }

    public Long getId() { return id; }
    public Student getStudent() { return student; }
    public String getSubject() { return subject; }
    public LocalDate getLessonDate() { return lessonDate; }
    public AttendanceStatus getStatus() { return status; }
    public void setStatus(AttendanceStatus status) { this.status = status; }
}