package com.voscorp.vosmlp.schedule;

// дни, когда пар нет (на РК пары идут, поэтому РК тут не нужен)
public enum NoLessonType {
    HOLIDAY("Праздник"),
    PRACTICE("Практика"),
    ATTESTATION("Аттестация");

    private final String label;

    NoLessonType(String label) { this.label = label; }

    public String getLabel() { return label; }
}