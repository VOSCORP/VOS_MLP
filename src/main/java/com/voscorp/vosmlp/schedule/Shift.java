package com.voscorp.vosmlp.schedule;

public enum Shift {
    FIRST("1 смена", 8 * 60),
    SECOND("2 смена", 13 * 60);

    private final String label;
    private final int startMinute; // минута суток, с которой начинается смена

    Shift(String label, int startMinute) {
        this.label = label;
        this.startMinute = startMinute;
    }

    public String getLabel() { return label; }
    public int getStartMinute() { return startMinute; }

    // в какую смену группа переходит в следующем семестре (1 -> 2, 2 -> 1)
    public Shift next() { return this == FIRST ? SECOND : FIRST; }
}