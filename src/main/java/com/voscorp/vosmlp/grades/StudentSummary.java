package com.voscorp.vosmlp.grades;

public class StudentSummary {
    private final String name;
    private final String groupName;
    private final String averages;
    private final String scholarship; // YES, NO или NO_DATA

    public StudentSummary(String name, String groupName, String averages, String scholarship) {
        this.name = name;
        this.groupName = groupName;
        this.averages = averages;
        this.scholarship = scholarship;
    }

    public String getName() { return name; }
    public String getGroupName() { return groupName; }
    public String getAverages() { return averages; }
    public String getScholarship() { return scholarship; }
}