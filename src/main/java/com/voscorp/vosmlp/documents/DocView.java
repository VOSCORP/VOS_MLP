package com.voscorp.vosmlp.documents;

public class DocView {
    private final Long id;
    private final String typeLabel;
    private final String title;
    private final String number;
    private final String issuedBy;
    private final String issuedText;
    private final String expiryText;
    private final String statusLabel;
    private final String statusState;
    private final String sourceLabel;
    private final String note;
    private final String fileName;
    private final boolean canSync;
    private final boolean canDelete;

    public DocView(Long id, String typeLabel, String title, String number, String issuedBy, String issuedText,
                   String expiryText, String statusLabel, String statusState, String sourceLabel, String note,
                   String fileName, boolean canSync, boolean canDelete) {
        this.id = id;
        this.typeLabel = typeLabel;
        this.title = title;
        this.number = number;
        this.issuedBy = issuedBy;
        this.issuedText = issuedText;
        this.expiryText = expiryText;
        this.statusLabel = statusLabel;
        this.statusState = statusState;
        this.sourceLabel = sourceLabel;
        this.note = note;
        this.fileName = fileName;
        this.canSync = canSync;
        this.canDelete = canDelete;
    }

    public Long getId() { return id; }
    public String getTypeLabel() { return typeLabel; }
    public String getTitle() { return title; }
    public String getNumber() { return number; }
    public String getIssuedBy() { return issuedBy; }
    public String getIssuedText() { return issuedText; }
    public String getExpiryText() { return expiryText; }
    public String getStatusLabel() { return statusLabel; }
    public String getStatusState() { return statusState; }
    public String getSourceLabel() { return sourceLabel; }
    public String getNote() { return note; }
    public String getFileName() { return fileName; }
    public boolean isCanSync() { return canSync; }
    public boolean isCanDelete() { return canDelete; }
    public String getDownloadUrl() { return "/documents/" + id + "/download"; }
    public String getSyncUrl() { return canSync ? "/documents/" + id + "/sync" : ""; }
}