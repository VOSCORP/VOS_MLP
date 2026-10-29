package com.voscorp.vosmlp.journal;

import java.util.List;

public class ScholarshipResult {
    private final String status; // YES, NO или NO_DATA
    private final List<Line> lines;

    public ScholarshipResult(String status, List<Line> lines) {
        this.status = status;
        this.lines = lines;
    }

    public String getStatus() { return status; }
    public List<Line> getLines() { return lines; }

    public static class Line {
        private final String subject;
        private final String average;
        private final int count;
        private final int need;
        private final boolean ok;

        public Line(String subject, String average, int count, int need, boolean ok) {
            this.subject = subject;
            this.average = average;
            this.count = count;
            this.need = need;
            this.ok = ok;
        }

        public String getSubject() { return subject; }
        public String getAverage() { return average; }
        public int getCount() { return count; }
        public int getNeed() { return need; }
        public boolean isOk() { return ok; }
    }
}