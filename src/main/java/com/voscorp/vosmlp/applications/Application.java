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

    @ManyToOne
    private AppUser recipient; // кому адресована (например, преподаватель), может быть пусто

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

    public Application(ApplicationType type, AppUser applicant, String title, String comment, String details,
                       AppUser recipient) {
        this(type, applicant, title, comment, details);
        this.recipient = recipient;
    }

    public void markSigned(String trace) {
        this.signTrace = trace;
        this.signedAt = LocalDateTime.now();
        this.status = ApplicationStatus.PENDING_DECISION;
    }

    private void decide(ApplicationStatus newStatus, String by, String note) {
        this.status = newStatus;
        this.decidedBy = by;
        this.decidedAt = LocalDateTime.now();
        this.decisionNote = note;
    }

    public void approve(String by, String note) { decide(ApplicationStatus.APPROVED, by, note); }
    public void reject(String by, String note) { decide(ApplicationStatus.REJECTED, by, note); }
    public void forward(String by, String note) { decide(ApplicationStatus.FORWARDED, by, note); }
    public void returnToTeacher(String by, String note) { decide(ApplicationStatus.PENDING_DECISION, by, note); }

    public Long getId() { return id; }
    public ApplicationType getType() { return type; }
    public AppUser getApplicant() { return applicant; }
    public AppUser getRecipient() { return recipient; }
    public ApplicationStatus getStatus() { return status; }
    public String getTitle() { return title; }
    public String getComment() { return comment; }
    public String getDetails() { return details; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getSignTrace() { return signTrace; }
    public String getDecidedBy() { return decidedBy; }
    public String getDecisionNote() { return decisionNote; }
}