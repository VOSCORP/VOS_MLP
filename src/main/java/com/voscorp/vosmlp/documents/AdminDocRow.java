package com.voscorp.vosmlp.documents;

public class AdminDocRow {
    private final Long id;
    private final String ownerName;
    private final String typeLabel;
    private final String fileName;
    private final String uploadedText;
    private final String uploadedBy;

    public AdminDocRow(Long id, String ownerName, String typeLabel, String fileName,
                       String uploadedText, String uploadedBy) {
        this.id = id;
        this.ownerName = ownerName;
        this.typeLabel = typeLabel;
        this.fileName = fileName;
        this.uploadedText = uploadedText;
        this.uploadedBy = uploadedBy;
    }

    public Long getId() { return id; }
    public String getOwnerName() { return ownerName; }
    public String getTypeLabel() { return typeLabel; }
    public String getFileName() { return fileName; }
    public String getUploadedText() { return uploadedText; }
    public String getUploadedBy() { return uploadedBy; }
}