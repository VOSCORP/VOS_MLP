package com.voscorp.vosmlp.applications;

import com.voscorp.vosmlp.auth.AppUser;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class Application {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "application_type")
    private ApplicationType type;

    @ManyToOne
    private AppUser applicant;

    @Enumerated(EnumType.STRING)
    @Column(name = "app_status")
    private ApplicationStatus status;

    @Column(length = 300)
    private String title;

    @Column(length = 1000, name = "applicant_comment")
    private String comment;

    @Column(length = 4000)
    private String details;

    private LocalDateTime createdAt;
    private LocalDateTime signedAt;
    private String signTrace;
    private String decidedBy;
    private LocalDateTime decidedAt;

    @Column(length = 500)
    private String decisionNote;

    protected Application() {}

    public Application(ApplicationType type, AppUser applicant, String title, String comment, String details) {
        this.type = type;
        this.applicant = applicant;
        this.title = title;
        this.comment = comment;
        this.details = details;
        this.status = ApplicationStatus.PENDING_SIGNATURE;
        this.createdAt = LocalDateTime.now();
    }

    public void markSigned(String trace) {
        this.signTrace = trace;
        this.signedAt = LocalDateTime.now();
        this.status = ApplicationStatus.PENDING_DECISION;
    }

    public void approve(String by, String note) {
        this.status = ApplicationStatus.APPROVED;
        this.decidedBy = by;
        this.decidedAt = LocalDateTime.now();
        this.decisionNote = note;
    }

    public void reject(String by, String note) {
        this.status = ApplicationStatus.REJECTED;
        this.decidedBy = by;
        this.decidedAt = LocalDateTime.now();
        this.decisionNote = note;
    }

    public Long getId() { return id; }
    public ApplicationType getType() { return type; }
    public AppUser getApplicant() { return applicant; }
    public ApplicationStatus getStatus() { return status; }
    public String getTitle() { return title; }
    public String getComment() { return comment; }
    public String getDetails() { return details; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getSignTrace() { return signTrace; }
    public String getDecidedBy() { return decidedBy; }
    public String getDecisionNote() { return decisionNote; }
}