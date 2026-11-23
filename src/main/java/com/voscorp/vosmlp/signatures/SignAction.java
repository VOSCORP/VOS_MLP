package com.voscorp.vosmlp.signatures;

// Виды действий, которые подписываются через VOS Gov
public enum SignAction {
    ATTENDANCE_CHANGE("Заявка на изменение посещаемости"),
    JOURNAL_ACCESS("Заявка на управление журналом");

    private final String label;

    SignAction(String label) { this.label = label; }

    public String getLabel() { return label; }
}