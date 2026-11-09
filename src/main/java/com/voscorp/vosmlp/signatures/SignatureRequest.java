package com.voscorp.vosmlp.signatures;

import com.voscorp.vosmlp.auth.AppUser;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class SignatureRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "sign_action")
    private SignAction action;

    private Long targetId;

    @ManyToOne
    private AppUser requester;

    @Column(length = 500)
    private String summary;

    private String returnUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "sign_status")
    private SignStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime signedAt;
    private LocalDateTime confirmedAt;
    private String traceNumber;

    protected SignatureRequest() {}

    public SignatureRequest(String token, SignAction action, Long targetId, AppUser requester,
                            String summary, String returnUrl) {
        this.token = token;
        this.action = action;
        this.targetId = targetId;
        this.requester = requester;
        this.summary = summary;
        this.returnUrl = returnUrl;
        this.status = SignStatus.CREATED;
        this.createdAt = LocalDateTime.now();
    }

    public void sign() {
        this.status = SignStatus.SIGNED;
        this.signedAt = LocalDateTime.now();
    }

    public void confirm(String traceNumber) {
        this.status = SignStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
        this.traceNumber = traceNumber;
    }

    public void cancel() { this.status = SignStatus.CANCELLED; }

    public Long getId() { return id; }
    public String getToken() { return token; }
    public SignAction getAction() { return action; }
    public Long getTargetId() { return targetId; }
    public AppUser getRequester() { return requester; }
    public String getSummary() { return summary; }
    public String getReturnUrl() { return returnUrl; }
    public SignStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getTraceNumber() { return traceNumber; }
}