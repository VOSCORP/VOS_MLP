package com.voscorp.vosmlp.applications;

public enum ApplicationStatus {
    PENDING_SIGNATURE("Ждёт подписи", "warn"),
    PENDING_DECISION("На рассмотрении", "warn"),
    APPROVED("Одобрена", "ok"),
    REJECTED("Отклонена", "bad");

    private final String label;
    private final String css;

    ApplicationStatus(String label, String css) {
        this.label = label;
        this.css = css;
    }

    public String getLabel() { return label; }
    public String getCss() { return css; }
}