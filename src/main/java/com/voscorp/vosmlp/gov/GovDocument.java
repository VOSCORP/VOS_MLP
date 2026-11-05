package com.voscorp.vosmlp.gov;

import com.voscorp.vosmlp.auth.AppUser;
import com.voscorp.vosmlp.documents.DocType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

// Временная заглушка гос базы: документы людей. Позже заменится настоящим VOS Gov.
@Entity
public class GovDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private AppUser owner;

    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type")
    private DocType type;

    private String number;
    private String issuedBy;
    private LocalDate issuedDate;
    private LocalDate expiryDate;
    private int version;
    private String fileName;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(length = 5_300_000)
    private byte[] content;

    protected GovDocument() {}

    public GovDocument(AppUser owner, DocType type, String number, String issuedBy, LocalDate issuedDate,
                       LocalDate expiryDate, int version, String fileName, byte[] content) {
        this.owner = owner;
        this.type = type;
        this.number = number;
        this.issuedBy = issuedBy;
        this.issuedDate = issuedDate;
        this.expiryDate = expiryDate;
        this.version = version;
        this.fileName = fileName;
        this.content = content;
    }

    public Long getId() { return id; }
    public AppUser getOwner() { return owner; }
    public DocType getType() { return type; }
    public String getNumber() { return number; }
    public String getIssuedBy() { return issuedBy; }
    public LocalDate getIssuedDate() { return issuedDate; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public int getVersion() { return version; }
    public String getFileName() { return fileName; }
    public byte[] getContent() { return content; }
}