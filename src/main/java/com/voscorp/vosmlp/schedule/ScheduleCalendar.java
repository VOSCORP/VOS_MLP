package com.voscorp.vosmlp.schedule;

import com.voscorp.vosmlp.journal.StudyGroup;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// Календарь семестра: в какие даты вообще могут быть пары
@Service
public class ScheduleCalendar {
    private final NoLessonPeriodRepository periods;

    public ScheduleCalendar(NoLessonPeriodRepository periods) {
        this.periods = periods;
    }

    // Все даты семестра, когда пары возможны: Пн–Пт, не праздник, не практика и не аттестация.
    // group = null значит «только то, что касается всего колледжа».
    public List<LocalDate> teachingDates(Semester semester, StudyGroup group) {
        List<NoLessonPeriod> blocked = new ArrayList<>();
        for (NoLessonPeriod p : periods.findBySemester(semester)) {
            if (p.getStudyGroup() == null || (group != null && p.appliesTo(group))) {
                blocked.add(p);
            }
        }

        List<LocalDate> result = new ArrayList<>();
        LocalDate d = semester.getStartDate();
        while (!d.isAfter(semester.getEndDate())) {
            boolean weekday = d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY;
            boolean free = true;
            for (NoLessonPeriod p : blocked) {
                if (p.covers(d)) { free = false; break; }
            }
            if (weekday && free) { result.add(d); }
            d = d.plusDays(1);
        }
        return result;
    }

    // Сколько реальных занятий за семестр даст слот «день недели + режим недель»
    public int countSlot(Semester semester, StudyGroup group, DayOfWeek day, WeekMode mode) {
        int count = 0;
        for (LocalDate d : teachingDates(semester, group)) {
            if (d.getDayOfWeek() == day && mode.matchesWeek(semester.weekNo(d))) { count++; }
        }
        return count;
    }
}