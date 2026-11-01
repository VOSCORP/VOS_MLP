package com.voscorp.vosmlp.attendance;

import com.voscorp.vosmlp.journal.JournalColumn;
import com.voscorp.vosmlp.students.Student;
import jakarta.persistence.*;

import java.time.LocalDateTime;

// Одна отметка посещаемости. Нет записи = студент присутствовал.
@Entity
public class AttendanceRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Student student;

    @ManyToOne
    private JournalColumn journalColumn; // занятие

    @Enumerated(EnumType.STRING)
    @Column(name = "att_mark")
    private AttendanceMark mark;

    private Integer lateMinutes;
    private boolean excused; // уважительная причина (для пропуска)
    private String markedBy;
    private LocalDateTime markedAt;

    protected AttendanceRecord() {}

    public AttendanceRecord(Student student, JournalColumn journalColumn, AttendanceMark mark,
                            Integer lateMinutes, boolean excused, String markedBy) {
        this.student = student;
        this.journalColumn = journalColumn;
        this.mark = mark;
        this.lateMinutes = lateMinutes;
        this.excused = excused;
        this.markedBy = markedBy;
        this.markedAt = LocalDateTime.now();
    }

    public void update(AttendanceMark mark, Integer lateMinutes, boolean excused, String markedBy) {
        this.mark = mark;
        this.lateMinutes = lateMinutes;
        this.excused = excused;
        this.markedBy = markedBy;
        this.markedAt = LocalDateTime.now();
    }

    // короткое описание для аудита и уведомлений
    public static String describe(AttendanceRecord r) {
        if (r == null) { return "присутствовал"; }
        if (r.mark == AttendanceMark.LATE) { return "опоздание " + r.lateMinutes + " мин"; }
        return r.excused ? "пропуск (с причиной)" : "пропуск (без причины)";
    }

    public Long getId() { return id; }
    public Student getStudent() { return student; }
    public JournalColumn getJournalColumn() { return journalColumn; }
    public AttendanceMark getMark() { return mark; }
    public Integer getLateMinutes() { return lateMinutes; }
    public boolean isExcused() { return excused; }
    public String getMarkedBy() { return markedBy; }
    public LocalDateTime getMarkedAt() { return markedAt; }
}