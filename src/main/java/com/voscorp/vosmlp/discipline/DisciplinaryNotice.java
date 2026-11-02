package com.voscorp.vosmlp.discipline;

import com.voscorp.vosmlp.students.Student;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
public class DisciplinaryNotice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Student student;

    private Long applicationId; // номер связанной заявки (может быть пусто)

    @Column(length = 400)
    private String reason;

    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private NoticeStatus status;

    protected DisciplinaryNotice() {}

    public DisciplinaryNotice(Student student, Long applicationId, String reason) {
        this.student = student;
        this.applicationId = applicationId;
        this.reason = reason;
        this.createdAt = LocalDateTime.now();
        this.status = NoticeStatus.ACTIVE;
    }

    public Long getId() { return id; }
    public Student getStudent() { return student; }
    public Long getApplicationId() { return applicationId; }
    public String getReason() { return reason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public NoticeStatus getStatus() { return status; }

    public String getCreatedAtText() {
        return createdAt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }
}