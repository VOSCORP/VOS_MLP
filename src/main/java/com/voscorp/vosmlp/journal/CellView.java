package com.voscorp.vosmlp.journal;

public class CellView {
    private final Long studentId;
    private final Long columnId;
    private final String status;
    private final String score;
    private final String lateMinutes;
    private final boolean excused;
    private final String text;
    private final String css;
    private final String title;

    public CellView(Long studentId, Long columnId, JournalEntry e) {
        String st = "NORMAL", sc = "", lm = "", t = "", c = "empty", ti = "";
        boolean ex = false;

        if (e != null) {
            st = e.getStatus().name();
            sc = e.getScore() == null ? "" : String.valueOf(e.getScore());
            lm = e.getLateMinutes() == null ? "" : String.valueOf(e.getLateMinutes());
            ex = e.isExcused();
            switch (e.getStatus()) {
                case LATE:
                    t = (e.getScore() != null ? e.getScore() + " · " : "") + e.getLateMinutes() + " мин";
                    c = "late";
                    ti = "Опоздание на " + e.getLateMinutes() + " мин";
                    break;
                case ABSENT:
                    t = "Н";
                    c = "absent";
                    ti = ex ? "Не пришёл, с причиной" : "Не пришёл, без причины";
                    break;
                case MAKEUP:
                    t = e.getScore() == null ? "отр." : e.getScore() + " (отр.)";
                    c = "makeup";
                    ti = "Отработка: максимум 80 баллов";
                    break;
                default:
                    t = sc;
                    c = "normal";
            }
        }

        this.studentId = studentId;
        this.columnId = columnId;
        this.status = st;
        this.score = sc;
        this.lateMinutes = lm;
        this.excused = ex;
        this.text = t;
        this.css = c;
        this.title = ti;
    }

    public Long getStudentId() { return studentId; }
    public Long getColumnId() { return columnId; }
    public String getStatus() { return status; }
    public String getScore() { return score; }
    public String getLateMinutes() { return lateMinutes; }
    public boolean isExcused() { return excused; }
    public String getText() { return text; }
    public String getCss() { return css; }
    public String getTitle() { return title; }
}