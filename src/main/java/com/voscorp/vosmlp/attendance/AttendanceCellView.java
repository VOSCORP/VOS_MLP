package com.voscorp.vosmlp.attendance;

public class AttendanceCellView {
    private final Long studentId;
    private final Long columnId;
    private final String state;   // PRESENT, LATE, ABSENT
    private final String minutes;
    private final boolean excused;
    private final String text;
    private final String css;
    private final String title;

    public AttendanceCellView(Long studentId, Long columnId, AttendanceRecord r) {
        String st = "PRESENT", mi = "", t = "", c = "empty", ti = "";
        boolean ex = false;
        if (r != null) {
            st = r.getMark().name();
            ex = r.isExcused();
            String who = r.getMarkedBy() == null ? "" : " · отметил(а): " + r.getMarkedBy();
            if (r.getMark() == AttendanceMark.LATE) {
                mi = String.valueOf(r.getLateMinutes());
                t = mi + " мин";
                c = "late";
                ti = "Опоздание на " + mi + " мин" + who;
            } else if (ex) {
                t = "Н (ув.)";
                c = "absent-excused";
                ti = "Не пришёл, с уважительной причиной" + who;
            } else {
                t = "Н";
                c = "absent";
                ti = "Не пришёл, без причины" + who;
            }
        }
        this.studentId = studentId;
        this.columnId = columnId;
        this.state = st;
        this.minutes = mi;
        this.excused = ex;
        this.text = t;
        this.css = c;
        this.title = ti;
    }

    public Long getStudentId() { return studentId; }
    public Long getColumnId() { return columnId; }
    public String getState() { return state; }
    public String getMinutes() { return minutes; }
    public boolean isExcused() { return excused; }
    public String getText() { return text; }
    public String getCss() { return css; }
    public String getTitle() { return title; }
}