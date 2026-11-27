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
    private final String categoryLabel;
    private final int priority;

    public ThreadView(Long id, String subject, String otherName, String otherRole, String updatedText,
                      boolean unread, String stateLabel, String stateClass, String categoryLabel, int priority) {
        this.id = id;
        this.subject = subject;
        this.otherName = otherName;
        this.otherRole = otherRole;
        this.updatedText = updatedText;
        this.unread = unread;
        this.stateLabel = stateLabel;
        this.stateClass = stateClass;
        this.categoryLabel = categoryLabel;
        this.priority = priority;
    }

    public Long getId() { return id; }
    public String getSubject() { return subject; }
    public String getOtherName() { return otherName; }
    public String getOtherRole() { return otherRole; }
    public String getUpdatedText() { return updatedText; }
    public boolean isUnread() { return unread; }
    public String getStateLabel() { return stateLabel; }
    public String getStateClass() { return stateClass; }
    public String getCategoryLabel() { return categoryLabel; }
    public int getPriority() { return priority; }

    // цвет письма: 1–3 голубой, 4–6 жёлтый, 7–8 оранжевый, 9 красный, 10 кроваво-красный
    public String getPrioClass() {
        if (priority >= 10) { return "prio-critical"; }
        if (priority == 9) { return "prio-urgent"; }
        if (priority >= 7) { return "prio-high"; }
        if (priority >= 4) { return "prio-mid"; }
        return "prio-low";
    }
}