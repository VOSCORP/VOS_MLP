package com.voscorp.vosmlp.documents;

import java.util.List;

public enum DocType {
    ID_CARD("Удостоверение личности", "У вас нет удостоверения личности в VOS Gov", true, false),
    DRIVER_LICENSE("Водительское удостоверение", "У вас нет водительского удостоверения", true, false),
    FOREIGN_PASSPORT("Загранпаспорт", "У вас нет загранпаспорта", true, false),
    BIRTH_CERTIFICATE("Метрика о рождении", "У вас нет метрики о рождении в VOS Gov", true, false),
    OTHER("Другой документ", "", false, false),
    COLLEGE_CONTRACT("Договор с колледжом", "Пока не прикреплён администрацией", false, true),
    SCHOOL_CERTIFICATE("Аттестат школы", "Пока не прикреплён администрацией", false, true),
    BANK_CONTRACT("Договор с банком", "Пока не прикреплён администрацией", false, true),
    BANK_ACCOUNT_CERT("Справка об открытии счёта", "Пока не прикреплена администрацией", false, true);

    private final String label;
    private final String missingText;
    private final boolean govType;
    private final boolean important;

    DocType(String label, String missingText, boolean govType, boolean important) {
        this.label = label;
        this.missingText = missingText;
        this.govType = govType;
        this.important = important;
    }

    public String getLabel() { return label; }
    public String getMissingText() { return missingText; }
    public boolean isGovType() { return govType; }
    public boolean isImportant() { return important; }

    public static List<DocType> govTypes() {
        return List.of(ID_CARD, DRIVER_LICENSE, FOREIGN_PASSPORT, BIRTH_CERTIFICATE);
    }

    public static List<DocType> importantTypes() {
        return List.of(COLLEGE_CONTRACT, SCHOOL_CERTIFICATE, BANK_CONTRACT, BANK_ACCOUNT_CERT);
    }
}