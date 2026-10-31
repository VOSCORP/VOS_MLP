package com.voscorp.vosmlp.journal;

import java.util.List;

public class StudentDashboard {
    private final boolean gotToday;
    private final List<String> todayLines;
    private final List<String> bestLines;
    private final int chancePercent;
    private final String chanceText;
    private final String chanceState; // ok, warn, bad
    private final int attendancePercent;
    private final String attendanceState; // ok, warn, bad

    public StudentDashboard(boolean gotToday, List<String> todayLines, List<String> bestLines,
                            int chancePercent, String chanceText, String chanceState,
                            int attendancePercent, String attendanceState) {
        this.gotToday = gotToday;
        this.todayLines = todayLines;
        this.bestLines = bestLines;
        this.chancePercent = chancePercent;
        this.chanceText = chanceText;
        this.chanceState = chanceState;
        this.attendancePercent = attendancePercent;
        this.attendanceState = attendanceState;
    }

    public boolean isGotToday() { return gotToday; }
    public List<String> getTodayLines() { return todayLines; }
    public List<String> getBestLines() { return bestLines; }
    public int getChancePercent() { return chancePercent; }
    public String getChanceText() { return chanceText; }
    public String getChanceState() { return chanceState; }
    public int getAttendancePercent() { return attendancePercent; }
    public String getAttendanceState() { return attendanceState; }
}