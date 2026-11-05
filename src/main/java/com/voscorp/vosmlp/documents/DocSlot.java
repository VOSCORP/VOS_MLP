package com.voscorp.vosmlp.documents;

// Место под документ: либо документ есть, либо показываем, что его нет
public class DocSlot {
    private final String typeCode;
    private final String typeLabel;
    private final String missingText;
    private final boolean govType;
    private final DocView view; // null, если документа нет

    public DocSlot(String typeCode, String typeLabel, String missingText, boolean govType, DocView view) {
        this.typeCode = typeCode;
        this.typeLabel = typeLabel;
        this.missingText = missingText;
        this.govType = govType;
        this.view = view;
    }

    public String getTypeCode() { return typeCode; }
    public String getTypeLabel() { return typeLabel; }
    public String getMissingText() { return missingText; }
    public boolean isGovType() { return govType; }
    public DocView getView() { return view; }
    public boolean isPresent() { return view != null; }
}