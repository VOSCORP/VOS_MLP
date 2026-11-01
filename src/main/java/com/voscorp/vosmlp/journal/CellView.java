package com.voscorp.vosmlp.journal;

public class CellView {
    private final Long studentId;
    private final Long columnId;
    private final String status;
    private final String score;
    private final String text;
    private final String css;
    private final String title;

    public CellView(Long studentId, Long columnId, JournalEntry e) {
        String st = "NORMAL", sc = "", t = "", c = "empty", ti = "";

        if (e != null) {
            st = e.getStatus().name();
            sc = e.getScore() == null ? "" : String.valueOf(e.getScore());
            if (e.getStatus() == CellStatus.MAKEUP) {
                t = e.getScore() == null ? "отр." : e.getScore() + " (отр.)";
                c = "makeup";
                ti = "Отработка: максимум 80 баллов";
            } else {
                t = sc;
                c = "normal";
            }
        }

        this.studentId = studentId;
        this.columnId = columnId;
        this.status = st;
        this.score = sc;
        this.text = t;
        this.css = c;
        this.title = ti;
    }

    public Long getStudentId() { return studentId; }
    public Long getColumnId() { return columnId; }
    public String getStatus() { return status; }
    public String getScore() { return score; }
    public String getText() { return text; }
    public String getCss() { return css; }
    public String getTitle() { return title; }
}