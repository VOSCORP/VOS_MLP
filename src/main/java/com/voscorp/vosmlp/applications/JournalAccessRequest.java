package com.voscorp.vosmlp.applications;

import com.voscorp.vosmlp.journal.AccessType;
import com.voscorp.vosmlp.journal.StudyGroup;
import com.voscorp.vosmlp.journal.Subject;
import jakarta.persistence.*;

import java.util.LinkedHashSet;
import java.util.Set;

// Данные заявки старосты: какая таблица, какие предметы, какой доступ
@Entity
public class JournalAccessRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Application application;

    @ManyToOne
    private StudyGroup targetGroup;

    private boolean ownGroup;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "journal_request_subjects")
    private Set<Subject> subjects = new LinkedHashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "access_type")
    private AccessType accessType;

    protected JournalAccessRequest() {}

    public JournalAccessRequest(Application application, StudyGroup targetGroup, boolean ownGroup,
                                Set<Subject> subjects, AccessType accessType) {
        this.application = application;
        this.targetGroup = targetGroup;
        this.ownGroup = ownGroup;
        this.subjects = subjects;
        this.accessType = accessType;
    }

    public Long getId() { return id; }
    public Application getApplication() { return application; }
    public StudyGroup getTargetGroup() { return targetGroup; }
    public boolean isOwnGroup() { return ownGroup; }
    public Set<Subject> getSubjects() { return subjects; }
    public AccessType getAccessType() { return accessType; }
}