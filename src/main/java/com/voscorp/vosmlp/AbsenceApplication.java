package com.voscorp.vosmlp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class AbsenceApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Attendance attendance;

    private String reason;
    private String documentType;
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    @Enumerated(EnumType.STRING)
    private MedicalResult medicalResult;

    protected AbsenceApplication() {}

    public AbsenceApplication(Attendance attendance, String reason, String documentType) {
        this.attendance = attendance;
        this.reason = reason;
        this.documentType = documentType;
        this.createdAt = LocalDateTime.now();
        this.status = ApplicationStatus.PENDING;
    }

    public Long getId() { return id; }
    public Attendance getAttendance() { return attendance; }
    public String getReason() { return reason; }
    public String getDocumentType() { return documentType; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public ApplicationStatus getStatus() { return status; }
    public MedicalResult getMedicalResult() { return medicalResult; }
    public void setStatus(ApplicationStatus status) { this.status = status; }
    public void setMedicalResult(MedicalResult medicalResult) { this.medicalResult = medicalResult; }
}