package com.voscorp.vosmlp.schedule;

import com.voscorp.vosmlp.journal.StudyGroup;
import jakarta.persistence.*;

import java.time.LocalDate;

// Период, когда пар нет: праздник, практика, аттестация
@Entity
public class NoLessonPeriod {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Semester semester;

    @ManyToOne
    private StudyGroup studyGroup; // пусто = для всего колледжа

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type")
    private NoLessonType type;

    private LocalDate dateFrom;
    private LocalDate dateTo;
    private String title;

    protected NoLessonPeriod() {}

    public NoLessonPeriod(Semester semester, StudyGroup studyGroup, NoLessonType type,
                          LocalDate dateFrom, LocalDate dateTo, String title) {
        this.semester = semester;
        this.studyGroup = studyGroup;
        this.type = type;
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
        this.title = title;
    }

    public boolean covers(LocalDate date) {
        return !date.isBefore(dateFrom) && !date.isAfter(dateTo);
    }

    // касается ли период этой группы
    public boolean appliesTo(StudyGroup group) {
        return studyGroup == null || studyGroup.getId().equals(group.getId());
    }

    public Long getId() { return id; }
    public Semester getSemester() { return semester; }
    public StudyGroup getStudyGroup() { return studyGroup; }
    public NoLessonType getType() { return type; }
    public LocalDate getDateFrom() { return dateFrom; }
    public LocalDate getDateTo() { return dateTo; }
    public String getTitle() { return title; }
}