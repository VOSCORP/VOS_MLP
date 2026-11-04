package com.voscorp.vosmlp.attendance;

import java.util.List;

public class AttendanceSummary {
    private final List<AlertRow> alerts;
    private final List<LiveRow> live;
    private final List<LateRank> lateRanks;
    private final List<AbsenceRank> absenceRanks;

    public AttendanceSummary(List<AlertRow> alerts, List<LiveRow> live, List<LateRank> lateRanks,
                             List<AbsenceRank> absenceRanks) {
        this.alerts = alerts;
        this.live = live;
        this.lateRanks = lateRanks;
        this.absenceRanks = absenceRanks;
    }

    public List<AlertRow> getAlerts() { return alerts; }
    public List<LiveRow> getLive() { return live; }
    public List<LateRank> getLateRanks() { return lateRanks; }
    public List<AbsenceRank> getAbsenceRanks() { return absenceRanks; }

    public static class AlertRow {
        private final String studentName, groupName, ruleLabel, details, dateText, mailSubject, mailBody;
        private final Long accountId;

        public AlertRow(String studentName, String groupName, String ruleLabel, String details, String dateText,
                        Long accountId, String mailSubject, String mailBody) {
            this.studentName = studentName;
            this.groupName = groupName;
            this.ruleLabel = ruleLabel;
            this.details = details;
            this.dateText = dateText;
            this.accountId = accountId;
            this.mailSubject = mailSubject;
            this.mailBody = mailBody;
        }

        public String getStudentName() { return studentName; }
        public String getGroupName() { return groupName; }
        public String getRuleLabel() { return ruleLabel; }
        public String getDetails() { return details; }
        public String getDateText() { return dateText; }
        public Long getAccountId() { return accountId; }
        public String getMailSubject() { return mailSubject; }
        public String getMailBody() { return mailBody; }
    }

    public static class LiveRow {
        private final String studentName, groupName, mailSubject, mailBody;
        private final List<String> lines;
        private final Long accountId;

        public LiveRow(String studentName, String groupName, List<String> lines, Long accountId,
                       String mailSubject, String mailBody) {
            this.studentName = studentName;
            this.groupName = groupName;
            this.lines = lines;
            this.accountId = accountId;
            this.mailSubject = mailSubject;
            this.mailBody = mailBody;
        }

        public String getStudentName() { return studentName; }
        public String getGroupName() { return groupName; }
        public List<String> getLines() { return lines; }
        public Long getAccountId() { return accountId; }
        public String getMailSubject() { return mailSubject; }
        public String getMailBody() { return mailBody; }
    }

    public static class LateRank {
        private final String groupName, subjectName;
        private final List<String> lines;

        public LateRank(String groupName, String subjectName, List<String> lines) {
            this.groupName = groupName;
            this.subjectName = subjectName;
            this.lines = lines;
        }

        public String getGroupName() { return groupName; }
        public String getSubjectName() { return subjectName; }
        public List<String> getLines() { return lines; }
    }

    public static class AbsenceRank {
        private final String studentName, groupName;
        private final int absences, unexcused, excused, percent;

        public AbsenceRank(String studentName, String groupName, int absences, int unexcused, int excused, int percent) {
            this.studentName = studentName;
            this.groupName = groupName;
            this.absences = absences;
            this.unexcused = unexcused;
            this.excused = excused;
            this.percent = percent;
        }

        public String getStudentName() { return studentName; }
        public String getGroupName() { return groupName; }
        public int getAbsences() { return absences; }
        public int getUnexcused() { return unexcused; }
        public int getExcused() { return excused; }
        public int getPercent() { return percent; }
    }
}