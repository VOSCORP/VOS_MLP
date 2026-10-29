package com.voscorp.vosmlp.journal;

import java.util.List;

public class JournalRow {
    private final Long studentId;
    private final String name;
    private final List<CellView> cells;
    private final String average;
    private final String finalScore;
    private final boolean eligible;

    public JournalRow(Long studentId, String name, List<CellView> cells,
                      String average, String finalScore, boolean eligible) {
        this.studentId = studentId;
        this.name = name;
        this.cells = cells;
        this.average = average;
        this.finalScore = finalScore;
        this.eligible = eligible;
    }

    public Long getStudentId() { return studentId; }
    public String getName() { return name; }
    public List<CellView> getCells() { return cells; }
    public String getAverage() { return average; }
    public String getFinalScore() { return finalScore; }
    public boolean isEligible() { return eligible; }
}