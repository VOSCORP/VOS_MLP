package com.voscorp.vosmlp.attendance;

public enum AlertRule {
    LATES_IN_SUBJECT("Опоздания по предмету"),
    ABSENCES_TOTAL("Много пропусков за период"),
    LOW_ATTENDANCE("Низкая посещаемость за период");

    private final String label;

    AlertRule(String label) { this.label = label; }

    public String getLabel() { return label; }
}