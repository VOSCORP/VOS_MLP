package com.voscorp.vosmlp.audit;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
public class AuditEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime createdAt;
    private String actorName;
    private String actorRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "audit_action")
    private AuditAction action;

    @Column(length = 400)
    private String target;

    @Column(length = 300)
    private String oldValue;

    @Column(length = 300)
    private String newValue;

    @Column(length = 600)
    private String note;

    private String outcome; // OK или DENIED

    protected AuditEvent() {}

    public AuditEvent(String actorName, String actorRole, AuditAction action, String target,
                      String oldValue, String newValue, String note, String outcome) {
        this.createdAt = LocalDateTime.now();
        this.actorName = actorName;
        this.actorRole = actorRole;
        this.action = action;
        this.target = target;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.note = note;
        this.outcome = outcome;
    }

    public Long getId() { return id; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getActorName() { return actorName; }
    public String getActorRole() { return actorRole; }
    public AuditAction getAction() { return action; }
    public String getTarget() { return target; }
    public String getOldValue() { return oldValue; }
    public String getNewValue() { return newValue; }
    public String getNote() { return note; }
    public String getOutcome() { return outcome; }

    public String getActionLabel() { return action.getLabel(); }

    public String getCreatedAtText() {
        return createdAt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
    }
}