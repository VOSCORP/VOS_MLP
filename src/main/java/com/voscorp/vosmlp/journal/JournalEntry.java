package com.voscorp.vosmlp.journal;

import com.voscorp.vosmlp.students.Student;
import jakarta.persistence.*;

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

    private Integer lateMinutes;
    private boolean excused;

    protected JournalEntry() {}

    public JournalEntry(Student student, JournalColumn journalColumn, Integer score,
                        CellStatus status, Integer lateMinutes, boolean excused) {
        this.student = student;
        this.journalColumn = journalColumn;
        this.score = score;
        this.status = status;
        this.lateMinutes = lateMinutes;
        this.excused = excused;
    }

    public Long getId() { return id; }
    public Student getStudent() { return student; }
    public JournalColumn getJournalColumn() { return journalColumn; }
    public Integer getScore() { return score; }
    public CellStatus getStatus() { return status; }
    public Integer getLateMinutes() { return lateMinutes; }
    public boolean isExcused() { return excused; }

    public void setScore(Integer score) { this.score = score; }
    public void setStatus(CellStatus status) { this.status = status; }
    public void setLateMinutes(Integer lateMinutes) { this.lateMinutes = lateMinutes; }
    public void setExcused(boolean excused) { this.excused = excused; }
}