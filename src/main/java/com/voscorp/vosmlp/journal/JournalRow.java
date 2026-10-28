package com.voscorp.vosmlp.journal;

import java.util.List;

public class JournalRow {
    private final String name;
    private final List<CellView> cells;
    private final String average;

    public JournalRow(String name, List<CellView> cells, String average) {
        this.name = name;
        this.cells = cells;
        this.average = average;
    }

    public String getName() { return name; }
    public List<CellView> getCells() { return cells; }
    public String getAverage() { return average; }
}
