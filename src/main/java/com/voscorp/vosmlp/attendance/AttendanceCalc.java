package com.voscorp.vosmlp.attendance;

import com.voscorp.vosmlp.journal.ColumnType;
import com.voscorp.vosmlp.journal.JournalColumn;
import com.voscorp.vosmlp.journal.JournalColumnRepository;
import com.voscorp.vosmlp.journal.StudyGroup;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collection;

@Service
public class AttendanceCalc {
    private final JournalColumnRepository columns;

    public AttendanceCalc(JournalColumnRepository columns) {
        this.columns = columns;
    }

    // Текущий период группы: между ключевыми датами (РК1, РК2, зачётная неделя, аттестация).
    // Возвращает {начало, конец}, обе границы не входят в период.
    public LocalDate[] currentPeriod(StudyGroup group) {
        LocalDate today = LocalDate.now();
        LocalDate start = LocalDate.MIN;
        LocalDate end = LocalDate.MAX;
        for (JournalColumn c : columns.findByStudyGroup(group)) {
            if (c.getType() == ColumnType.LESSON) { continue; }
            LocalDate d = c.getColumnDate();
            if (!d.isAfter(today) && d.isAfter(start)) { start = d; }
            if (d.isAfter(today) && d.isBefore(end)) { end = d; }
        }
        return new LocalDate[]{start, end};
    }

    public boolean inPeriod(JournalColumn column, LocalDate[] period) {
        LocalDate d = column.getColumnDate();
        return d.isAfter(period[0]) && d.isBefore(period[1]);
    }

    // {пропусков, опозданий, посещаемость %}.
    // Пропуск без причины = 1, опоздание = 0,5, пропуск с причиной не снижает посещаемость.
    public int[] stats(Collection<AttendanceRecord> list, int totalLessons) {
        int absences = 0;
        int lates = 0;
        double lost = 0;
        for (AttendanceRecord r : list) {
            if (r.getMark() == AttendanceMark.ABSENT) {
                absences++;
                if (!r.isExcused()) { lost += 1; }
            } else {
                lates++;
                lost += 0.5;
            }
        }
        int percent = totalLessons == 0 ? 100 : (int) Math.max(0, Math.round(100 - lost / totalLessons * 100));
        return new int[]{absences, lates, percent};
    }
}