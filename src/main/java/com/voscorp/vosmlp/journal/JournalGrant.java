package com.voscorp.vosmlp.journal;

import com.voscorp.vosmlp.auth.AppUser;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
public class JournalGrant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private AppUser teacher;

    @ManyToOne
    private AppUser starosta;

    @ManyToOne
    private StudyGroup studyGroup;

    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Subject> subjects = new LinkedHashSet<>();

    @Enumerated(EnumType.STRING)
    private AccessType accessType;

    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    private GrantStatus status;

    protected JournalGrant() {}

    public JournalGrant(AppUser teacher, AppUser starosta, StudyGroup studyGroup, Set<Subject> subjects,
                        AccessType accessType, LocalDateTime expiresAt) {
        this.teacher = teacher;
        this.starosta = starosta;
        this.studyGroup = studyGroup;
        this.subjects = subjects;
        this.accessType = accessType;
        this.expiresAt = expiresAt;
        this.createdAt = LocalDateTime.now();
        this.status = GrantStatus.ACTIVE;
    }

    // доступ действует, пока он не закрыт и не истёк срок (после «Выполнил» доступ действует до закрытия)
    public boolean isLive() {
        return (status == GrantStatus.ACTIVE || status == GrantStatus.DONE) && expiresAt.isAfter(LocalDateTime.now());
    }

    public Long getId() { return id; }
    public AppUser getTeacher() { return teacher; }
    public AppUser getStarosta() { return starosta; }
    public StudyGroup getStudyGroup() { return studyGroup; }
    public Set<Subject> getSubjects() { return subjects; }
    public AccessType getAccessType() { return accessType; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public GrantStatus getStatus() { return status; }
    public void setStatus(GrantStatus status) { this.status = status; }
}