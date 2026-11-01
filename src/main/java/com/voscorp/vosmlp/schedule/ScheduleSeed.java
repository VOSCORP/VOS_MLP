package com.voscorp.vosmlp.schedule;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Component
@Order(6)
public class ScheduleSeed implements CommandLineRunner {
    private final SemesterRepository semesters;
    private final NoLessonPeriodRepository periods;
    private final ScheduleCalendar calendar;

    public ScheduleSeed(SemesterRepository semesters, NoLessonPeriodRepository periods,
                        ScheduleCalendar calendar) {
        this.semesters = semesters;
        this.periods = periods;
        this.calendar = calendar;
    }

    @Override
    public void run(String... args) {
        if (semesters.count() > 0) { return; }

        Semester sem = semesters.save(new Semester("2026–2027 · 1 семестр",
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 2)));

        // 25 октября воскресенье, выходной переносится на понедельник
        periods.save(new NoLessonPeriod(sem, null, NoLessonType.HOLIDAY,
                LocalDate.of(2026, 10, 26), LocalDate.of(2026, 10, 26), "День Республики (перенос)"));

        // ВРЕМЕННАЯ проверка: сверь цифры в консоли с таблицей по неделям, потом блок можно удалить
        List<LocalDate> days = calendar.teachingDates(sem, null);
        System.out.println("[SCHEDULE] teaching days: " + days.size() + ", weeks: " + sem.weekNo(sem.getEndDate()));
        DayOfWeek[] week = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY};
        String[] names = {"Mon", "Tue", "Wed", "Thu", "Fri"};
        for (int i = 0; i < week.length; i++) {
            System.out.println("[SCHEDULE] " + names[i]
                    + ": every " + calendar.countSlot(sem, null, week[i], WeekMode.EVERY)
                    + ", left " + calendar.countSlot(sem, null, week[i], WeekMode.LEFT)
                    + ", right " + calendar.countSlot(sem, null, week[i], WeekMode.RIGHT));
        }
        System.out.println("[SCHEDULE] shift 1, Monday: "
                + BellSchedule.text(BellSchedule.interval(Shift.FIRST, DayOfWeek.MONDAY, 1)) + " ... "
                + BellSchedule.text(BellSchedule.interval(Shift.FIRST, DayOfWeek.MONDAY, 4)));
        System.out.println("[SCHEDULE] shift 1, Tuesday: curator hour "
                + BellSchedule.text(BellSchedule.interval(Shift.FIRST, DayOfWeek.TUESDAY, 0)) + ", pairs "
                + BellSchedule.text(BellSchedule.interval(Shift.FIRST, DayOfWeek.TUESDAY, 1)) + " ... "
                + BellSchedule.text(BellSchedule.interval(Shift.FIRST, DayOfWeek.TUESDAY, 4)));
    }
}