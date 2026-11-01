package com.voscorp.vosmlp.journal;

import com.voscorp.vosmlp.students.Student;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class JournalEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Student student;

    @ManyToOne
    private JournalColumn journalColumn;

    private Integer score; // пусто, если оценки нет

    @Enumerated(EnumType.STRING)
    private CellStatus status;

    private String gradedBy;
    private LocalDateTime gradedAt;

    protected JournalEntry() {}

    public JournalEntry(Student student, JournalColumn journalColumn, Integer score, CellStatus status) {
        this.student = student;
        this.journalColumn = journalColumn;
        this.score = score;
        this.status = status;
    }

    public Long getId() { return id; }
    public Student getStudent() { return student; }
    public JournalColumn getJournalColumn() { return journalColumn; }
    public Integer getScore() { return score; }
    public CellStatus getStatus() { return status; }
    public String getGradedBy() { return gradedBy; }
    public LocalDateTime getGradedAt() { return gradedAt; }

    public void setScore(Integer score) { this.score = score; }
    public void setStatus(CellStatus status) { this.status = status; }

    // запоминаем, кто и когда поставил оценку
    public void markGradedBy(String name) {
        this.gradedBy = name;
        this.gradedAt = LocalDateTime.now();
    }
}