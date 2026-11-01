package com.voscorp.vosmlp.schedule;

import java.time.DayOfWeek;

// Расписание звонков. Всё считаем в минутах от полуночи.
public class BellSchedule {
    public static final int PAIR_LENGTH = 80;     // одна пара
    public static final int BREAK_LENGTH = 10;    // перемена
    public static final int CURATOR_LENGTH = 25;  // кураторский час (нулевая пара)
    public static final int CURATOR_OFFSET = 30;  // на сколько кураторский час сдвигает пары

    // кураторский час бывает только во вторник
    public static boolean hasCuratorHour(DayOfWeek day) {
        return day == DayOfWeek.TUESDAY;
    }

    // {начало, конец} пары. Номер 0 = кураторский час, 1–4 = обычные пары
    public static int[] interval(Shift shift, DayOfWeek day, int pairNo) {
        int base = shift.getStartMinute();
        if (pairNo == 0) { return new int[]{base, base + CURATOR_LENGTH}; }
        int offset = hasCuratorHour(day) ? CURATOR_OFFSET : 0;
        int start = base + offset + (pairNo - 1) * (PAIR_LENGTH + BREAK_LENGTH);
        return new int[]{start, start + PAIR_LENGTH};
    }

    // пересекаются ли два промежутка времени (конец одного = начало другого не считается)
    public static boolean overlap(int[] a, int[] b) {
        return a[0] < b[1] && b[0] < a[1];
    }

    public static String text(int[] interval) {
        return time(interval[0]) + "-" + time(interval[1]);
    }

    private static String time(int minutes) {
        return String.format("%02d:%02d", minutes / 60, minutes % 60);
    }
}