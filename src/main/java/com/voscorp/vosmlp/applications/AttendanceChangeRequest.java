package com.voscorp.vosmlp.applications;

import com.voscorp.vosmlp.documents.MedicalResult;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
public class AttendanceChangeRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Application application;

    private String certificateNumber;
    private Long certificateDocumentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "medical_result")
    private MedicalResult medicalResult;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "attendance_change_records", joinColumns = @JoinColumn(name = "request_id"))
    @Column(name = "record_id")
    private List<Long> recordIds = new ArrayList<>();

    protected AttendanceChangeRequest() {}

    public AttendanceChangeRequest(Application application, String certificateNumber,
                                   Long certificateDocumentId, List<Long> recordIds) {
        this.application = application;
        this.certificateNumber = certificateNumber;
        this.certificateDocumentId = certificateDocumentId;
        this.recordIds = new ArrayList<>(recordIds);
    }

    public Long getId() { return id; }
    public Application getApplication() { return application; }
    public String getCertificateNumber() { return certificateNumber; }
    public Long getCertificateDocumentId() { return certificateDocumentId; }
    public MedicalResult getMedicalResult() { return medicalResult; }
    public void setMedicalResult(MedicalResult medicalResult) { this.medicalResult = medicalResult; }
    public List<Long> getRecordIds() { return recordIds; }
}