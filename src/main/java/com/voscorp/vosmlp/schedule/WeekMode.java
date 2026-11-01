package com.voscorp.vosmlp.schedule;

// как часто идёт занятие: каждую неделю или «мигающее» (половина ячейки в расписании)
public enum WeekMode {
    EVERY("Каждую неделю"),
    LEFT("Нечётные недели (левая половина)"),
    RIGHT("Чётные недели (правая половина)");

    private final String label;

    WeekMode(String label) { this.label = label; }

    public String getLabel() { return label; }

    // пересекаются ли два режима: «каждую неделю» пересекается со всеми, левая с правой нет
    public boolean overlaps(WeekMode other) {
        return this == EVERY || other == EVERY || this == other;
    }

    // идёт ли занятие на неделе с таким номером (первая неделя = левая)
    public boolean matchesWeek(int weekNo) {
        if (this == EVERY) { return true; }
        boolean odd = weekNo % 2 == 1;
        return this == LEFT ? odd : !odd;
    }
}