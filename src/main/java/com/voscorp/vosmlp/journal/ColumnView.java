package com.voscorp.vosmlp.journal;

public class ColumnView {
    private final String label;
    private final String task;
    private final String css;

    public ColumnView(String label, String task, String css) {
        this.label = label;
        this.task = task;
        this.css = css;
    }

    public String getLabel() { return label; }
    public String getTask() { return task; }
    public String getCss() { return css; }
}