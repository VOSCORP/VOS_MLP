package com.voscorp.vosmlp.journal;

public class CellView {
    private final String text;
    private final String css;
    private final String title;

    public CellView(String text, String css, String title) {
        this.text = text;
        this.css = css;
        this.title = title;
    }

    public String getText() { return text; }
    public String getCss() { return css; }
    public String getTitle() { return title; }
}