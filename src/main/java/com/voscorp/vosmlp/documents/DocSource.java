package com.voscorp.vosmlp.documents;

public enum DocSource {
    GOV("VOS Gov"),
    ADMIN("Администрация колледжа"),
    UPLOAD("Загружено пользователем");

    private final String label;

    DocSource(String label) { this.label = label; }

    public String getLabel() { return label; }
}