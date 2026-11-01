package com.voscorp.vosmlp.attendance;

import com.voscorp.vosmlp.journal.ColumnType;
import com.voscorp.vosmlp.journal.JournalColumn;
import com.voscorp.vosmlp.journal.JournalColumnRepository;
import com.voscorp.vosmlp.journal.StudyGroup;
import com.voscorp.vosmlp.journal.Subject;
import com.voscorp.vosmlp.students.Student;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Service
public class AttendanceCalc {
    private final JournalColumnRepository columns;
    private final AttendanceRecordRepository records;

    public AttendanceCalc(JournalColumnRepository columns, AttendanceRecordRepository records) {
        this.columns = columns;
        this.records = records;
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

    // Посещаемость студента по одному предмету за текущий период (для итогового балла в журнале)
    public int percentFor(Student student, StudyGroup group, Subject subject) {
        LocalDate[] period = currentPeriod(group);
        List<JournalColumn> lessons = columns.findByStudyGroupAndSubjectOrderByColumnDateAsc(group, subject).stream()
                .filter(c -> c.getType() == ColumnType.LESSON && inPeriod(c, period))
                .toList();
        return percentOf(student, lessons);
    }

    // Посещаемость студента по всем предметам группы за текущий период (для карточки студента)
    public int percentOverall(Student student) {
        StudyGroup group = student.getStudyGroup();
        if (group == null) { return 100; }
        LocalDate[] period = currentPeriod(group);
        List<JournalColumn> lessons = columns.findByStudyGroup(group).stream()
                .filter(c -> c.getType() == ColumnType.LESSON && inPeriod(c, period))
                .toList();
        return percentOf(student, lessons);
    }

    private int percentOf(Student student, List<JournalColumn> lessons) {
        if (lessons.isEmpty()) { return 100; }
        return stats(records.findByStudentAndJournalColumnIn(student, lessons), lessons.size())[2];
    }
}