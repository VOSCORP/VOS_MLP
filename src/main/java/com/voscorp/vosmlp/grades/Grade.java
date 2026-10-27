package com.voscorp.vosmlp.grades;

import com.voscorp.vosmlp.students.Student;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class Grade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Student student;

    private String subject;
    private String gradeType;
    private int score;
    private LocalDate gradeDate;

    protected Grade() {}

    public Grade(Student student, String subject, String gradeType, int score, LocalDate gradeDate) {
        this.student = student;
        this.subject = subject;
        this.gradeType = gradeType;
        this.score = score;
        this.gradeDate = gradeDate;
    }

    public Long getId() { return id; }
    public Student getStudent() { return student; }
    public String getSubject() { return subject; }
    public String getGradeType() { return gradeType; }
    public int getScore() { return score; }
    public LocalDate getGradeDate() { return gradeDate; }
}