package com.voscorp.vosmlp.discipline;

import com.voscorp.vosmlp.students.Student;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class ExpulsionProposal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Student student;

    private int noticeCount;
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private ProposalStatus status;

    protected ExpulsionProposal() {}

    public ExpulsionProposal(Student student, int noticeCount) {
        this.student = student;
        this.noticeCount = noticeCount;
        this.createdAt = LocalDateTime.now();
        this.status = ProposalStatus.PENDING_ADMIN_APPROVAL;
    }

    public Long getId() { return id; }
    public Student getStudent() { return student; }
    public int getNoticeCount() { return noticeCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public ProposalStatus getStatus() { return status; }
    public void setStatus(ProposalStatus status) { this.status = status; }
}