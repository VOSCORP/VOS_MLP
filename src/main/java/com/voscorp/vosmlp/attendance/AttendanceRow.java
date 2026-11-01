package com.voscorp.vosmlp.attendance;

import java.util.List;

public class AttendanceRow {
    private final Long studentId;
    private final String name;
    private final List<AttendanceCellView> cells;
    private final int absences;
    private final int lates;
    private final int percent;

    public AttendanceRow(Long studentId, String name, List<AttendanceCellView> cells,
                         int absences, int lates, int percent) {
        this.studentId = studentId;
        this.name = name;
        this.cells = cells;
        this.absences = absences;
        this.lates = lates;
        this.percent = percent;
    }

    public Long getStudentId() { return studentId; }
    public String getName() { return name; }
    public List<AttendanceCellView> getCells() { return cells; }
    public int getAbsences() { return absences; }
    public int getLates() { return lates; }
    public int getPercent() { return percent; }
}