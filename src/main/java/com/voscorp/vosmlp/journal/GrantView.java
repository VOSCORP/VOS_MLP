package com.voscorp.vosmlp.journal;

public class GrantView {
    private final Long id;
    private final String teacherName;
    private final String starostaName;
    private final String groupName;
    private final String subjectsText;
    private final String typeLabel;
    private final String expiresText;
    private final String statusLabel;
    private final String statusState; // ok, warn, bad
    private final boolean canClose;
    private final boolean canDone;

    public GrantView(Long id, String teacherName, String starostaName, String groupName, String subjectsText,
                     String typeLabel, String expiresText, String statusLabel, String statusState,
                     boolean canClose, boolean canDone) {
        this.id = id;
        this.teacherName = teacherName;
        this.starostaName = starostaName;
        this.groupName = groupName;
        this.subjectsText = subjectsText;
        this.typeLabel = typeLabel;
        this.expiresText = expiresText;
        this.statusLabel = statusLabel;
        this.statusState = statusState;
        this.canClose = canClose;
        this.canDone = canDone;
    }

    public Long getId() { return id; }
    public String getTeacherName() { return teacherName; }
    public String getStarostaName() { return starostaName; }
    public String getGroupName() { return groupName; }
    public String getSubjectsText() { return subjectsText; }
    public String getTypeLabel() { return typeLabel; }
    public String getExpiresText() { return expiresText; }
    public String getStatusLabel() { return statusLabel; }
    public String getStatusState() { return statusState; }
    public boolean isCanClose() { return canClose; }
    public boolean isCanDone() { return canDone; }
}