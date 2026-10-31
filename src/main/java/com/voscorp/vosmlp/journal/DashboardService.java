package com.voscorp.vosmlp.journal;

import com.voscorp.vosmlp.students.Student;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class DashboardService {
    private final JournalEntryRepository entries;
    private final JournalColumnRepository columns;
    private final JournalCalc calc;

    public DashboardService(JournalEntryRepository entries, JournalColumnRepository columns, JournalCalc calc) {
        this.entries = entries;
        this.columns = columns;
        this.calc = calc;
    }

    public StudentDashboard build(Student student) {
        List<JournalEntry> all = entries.findByStudent(student);
        LocalDate today = LocalDate.now();

        // 1. Оценка дня и лучшие баллы по предметам
        List<String> todayLines = new ArrayList<>();
        Map<String, Integer> best = new TreeMap<>();
        Map<String, int[]> bySubject = new TreeMap<>(); // [сумма, количество]
        for (JournalEntry e : all) {
            if (e.getScore() == null || e.getStatus() == CellStatus.ABSENT) { continue; }
            String subject = e.getJournalColumn().getSubject().getName();
            best.merge(subject, e.getScore(), Math::max);
            int[] a = bySubject.computeIfAbsent(subject, k -> new int[2]);
            a[0] += e.getScore();
            a[1]++;
            if (e.getGradedAt() != null && e.getGradedAt().toLocalDate().equals(today)) {
                String who = e.getGradedBy() == null ? "преподаватель" : e.getGradedBy();
                todayLines.add(e.getScore() + " · " + subject + " · " + who);
            }
        }
        List<String> bestLines = new ArrayList<>();
        best.forEach((subject, score) -> bestLines.add(subject + ": " + score));

        // 3. Посещаемость
        int attendance = attendancePercent(student, all, today);
        String attendanceState = attendance >= 90 ? "ok" : (attendance >= 70 ? "warn" : "bad");

        // 2. Шанс повышенной стипендии
        int chance;
        String text;
        String state;
        if (bySubject.isEmpty()) {
            chance = 0;
            text = "Пока нет оценок для расчёта.";
            state = "warn";
        } else {
            double sumAverages = 0;
            int need = 0;
            for (int[] a : bySubject.values()) {
                double avg = (double) a[0] / a[1];
                sumAverages += avg;
                if (avg < 90) { need += (int) Math.ceil((90.0 * a[1] - a[0]) / 10.0); }
            }
            double overall = sumAverages / bySubject.size();
            chance = (int) Math.min(100, Math.round(overall / 90.0 * 100));
            boolean eligible = "YES".equals(calc.scholarship(student).getStatus());

            if (attendance < 70) {
                chance = 0;
                text = "Нет возможности: посещаемость ниже 70%. Попробуйте в следующем семестре.";
                state = "bad";
            } else if (eligible) {
                chance = 100;
                text = "Вы проходите на повышенную стипендию.";
                state = "ok";
            } else if (chance < 70) {
                text = "Нет возможности. Попробуйте в следующем семестре.";
                state = "bad";
            } else {
                text = "Чтобы войти в повышенную стипендию, нужно ещё " + need + " " + plural(need) + " по 100.";
                state = "warn";
            }
        }

        return new StudentDashboard(!todayLines.isEmpty(), todayLines, bestLines,
                chance, text, state, attendance, attendanceState);
    }

    // Посещаемость = 100 − (пропуски без причины + 0,5 × опоздания) / (все занятия периода) × 100.
    // Период: между ключевыми датами (РК1, РК2, зачётная неделя, аттестация); после каждой даты счёт с 100%.
    private int attendancePercent(Student student, List<JournalEntry> all, LocalDate today) {
        StudyGroup group = student.getStudyGroup();
        if (group == null) { return 100; }
        List<JournalColumn> groupColumns = columns.findByStudyGroup(group);

        LocalDate start = LocalDate.MIN;
        LocalDate end = LocalDate.MAX;
        for (JournalColumn c : groupColumns) {
            if (c.getType() == ColumnType.LESSON) { continue; }
            LocalDate d = c.getColumnDate();
            if (!d.isAfter(today) && d.isAfter(start)) { start = d; }
            if (d.isAfter(today) && d.isBefore(end)) { end = d; }
        }

        Set<Long> lessonIds = new HashSet<>();
        for (JournalColumn c : groupColumns) {
            if (c.getType() == ColumnType.LESSON
                    && c.getColumnDate().isAfter(start) && c.getColumnDate().isBefore(end)) {
                lessonIds.add(c.getId());
            }
        }
        if (lessonIds.isEmpty()) { return 100; }

        double lost = 0;
        for (JournalEntry e : all) {
            if (!lessonIds.contains(e.getJournalColumn().getId())) { continue; }
            if (e.getStatus() == CellStatus.ABSENT && !e.isExcused()) { lost += 1; }
            else if (e.getStatus() == CellStatus.LATE) { lost += 0.5; }
        }
        return (int) Math.max(0, Math.round(100 - lost / lessonIds.size() * 100));
    }

    private String plural(int n) {
        int m10 = n % 10;
        int m100 = n % 100;
        if (m10 == 1 && m100 != 11) { return "оценка"; }
        if (m10 >= 2 && m10 <= 4 && (m100 < 12 || m100 > 14)) { return "оценки"; }
        return "оценок";
    }
}