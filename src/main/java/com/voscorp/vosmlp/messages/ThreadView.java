package com.voscorp.vosmlp.messages;

public class ThreadView {
    private final Long id;
    private final String subject;
    private final String otherName;
    private final String otherRole;
    private final String updatedText;
    private final boolean unread;
    private final String stateLabel;
    private final String stateClass;

    public ThreadView(Long id, String subject, String otherName, String otherRole, String updatedText,
                      boolean unread, String stateLabel, String stateClass) {
        this.id = id;
        this.subject = subject;
        this.otherName = otherName;
        this.otherRole = otherRole;
        this.updatedText = updatedText;
        this.unread = unread;
        this.stateLabel = stateLabel;
        this.stateClass = stateClass;
    }

    public Long getId() { return id; }
    public String getSubject() { return subject; }
    public String getOtherName() { return otherName; }
    public String getOtherRole() { return otherRole; }
    public String getUpdatedText() { return updatedText; }
    public boolean isUnread() { return unread; }
    public String getStateLabel() { return stateLabel; }
    public String getStateClass() { return stateClass; }
}