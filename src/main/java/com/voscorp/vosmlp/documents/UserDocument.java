package com.voscorp.vosmlp.documents;

import com.voscorp.vosmlp.auth.AppUser;
import com.voscorp.vosmlp.gov.GovDocument;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
public class UserDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private AppUser owner;

    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type")
    private DocType type;

    private String title;    // что это за документ (для типа «Другой»)
    private String purpose;  // для чего (для типа «Другой»)
    private String number;
    private String issuedBy;
    private LocalDate issuedDate;
    private LocalDate expiryDate;
    private int version;

    @Enumerated(EnumType.STRING)
    @Column(name = "doc_source")
    private DocSource source;

    private String fileName;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(length = 5_300_000)
    private byte[] content;

    private String uploadedBy;
    private LocalDateTime uploadedAt;

    protected UserDocument() {}

    public UserDocument(AppUser owner, DocType type, String title, String purpose, String number, String issuedBy,
                        LocalDate issuedDate, LocalDate expiryDate, int version, DocSource source,
                        String fileName, byte[] content, String uploadedBy) {
        this.owner = owner;
        this.type = type;
        this.title = title;
        this.purpose = purpose;
        this.number = number;
        this.issuedBy = issuedBy;
        this.issuedDate = issuedDate;
        this.expiryDate = expiryDate;
        this.version = version;
        this.source = source;
        this.fileName = fileName;
        this.content = content;
        this.uploadedBy = uploadedBy;
        this.uploadedAt = LocalDateTime.now();
    }

    // обновление копии из новой версии в VOS Gov
    public void updateFromGov(GovDocument g) {
        this.number = g.getNumber();
        this.issuedBy = g.getIssuedBy();
        this.issuedDate = g.getIssuedDate();
        this.expiryDate = g.getExpiryDate();
        this.version = g.getVersion();
        this.fileName = g.getFileName();
        this.content = g.getContent();
        this.uploadedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public AppUser getOwner() { return owner; }
    public DocType getType() { return type; }
    public String getTitle() { return title; }
    public String getPurpose() { return purpose; }
    public String getNumber() { return number; }
    public String getIssuedBy() { return issuedBy; }
    public LocalDate getIssuedDate() { return issuedDate; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public int getVersion() { return version; }
    public DocSource getSource() { return source; }
    public String getFileName() { return fileName; }
    public byte[] getContent() { return content; }
    public String getUploadedBy() { return uploadedBy; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
}