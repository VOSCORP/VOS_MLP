package com.voscorp.vosmlp.gov;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class GovSignature {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String traceNumber;
    private String signerName;
    private String actionLabel;

    @Column(length = 500)
    private String summary;

    private String system;
    private LocalDateTime signedAt;

    protected GovSignature() {}

    public GovSignature(String traceNumber, String signerName, String actionLabel, String summary, String system) {
        this.traceNumber = traceNumber;
        this.signerName = signerName;
        this.actionLabel = actionLabel;
        this.summary = summary;
        this.system = system;
        this.signedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getTraceNumber() { return traceNumber; }
    public String getSignerName() { return signerName; }
    public String getActionLabel() { return actionLabel; }
    public String getSummary() { return summary; }
    public String getSystem() { return system; }
    public LocalDateTime getSignedAt() { return signedAt; }
}