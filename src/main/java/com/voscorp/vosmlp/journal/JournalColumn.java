package com.voscorp.vosmlp.journal;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class JournalColumn {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private StudyGroup studyGroup;

    @ManyToOne
    private Subject subject;

    private LocalDate columnDate;

    @Enumerated(EnumType.STRING)
    private ColumnType type;

    private String task; // что сдать в этот день

    protected JournalColumn() {}

    public JournalColumn(StudyGroup studyGroup, Subject subject, LocalDate columnDate, ColumnType type, String task) {
        this.studyGroup = studyGroup;
        this.subject = subject;
        this.columnDate = columnDate;
        this.type = type;
        this.task = task;
    }

    public Long getId() { return id; }
    public StudyGroup getStudyGroup() { return studyGroup; }
    public Subject getSubject() { return subject; }
    public LocalDate getColumnDate() { return columnDate; }
    public ColumnType getType() { return type; }
    public String getTask() { return task; }
}