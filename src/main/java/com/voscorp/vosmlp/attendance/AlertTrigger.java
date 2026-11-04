package com.voscorp.vosmlp.attendance;

import java.time.LocalDate;

public class AlertTrigger {
    private final AlertRule rule;
    private final Long subjectKey; // номер предмета или 0
    private final String text;
    private final LocalDate periodKey;

    public AlertTrigger(AlertRule rule, Long subjectKey, String text, LocalDate periodKey) {
        this.rule = rule;
        this.subjectKey = subjectKey;
        this.text = text;
        this.periodKey = periodKey;
    }

    public AlertRule getRule() { return rule; }
    public Long getSubjectKey() { return subjectKey; }
    public String getText() { return text; }
    public LocalDate getPeriodKey() { return periodKey; }
}