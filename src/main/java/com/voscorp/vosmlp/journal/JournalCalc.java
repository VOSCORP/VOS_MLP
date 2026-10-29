package com.voscorp.vosmlp.journal;

import com.voscorp.vosmlp.students.Student;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class JournalCalc {
    private final SubjectRepository subjects;
    private final JournalColumnRepository columns;
    private final JournalEntryRepository entries;

    public JournalCalc(SubjectRepository subjects, JournalColumnRepository columns, JournalEntryRepository entries) {
        this.subjects = subjects;
        this.columns = columns;
        this.entries = entries;
    }

    // Итог = средний × (0,9 + 0,1 × посещаемость).
    // Пропуск без причины = 1, опоздание = 0,5, пропуск с причиной не считается.
    public String finalScore(Collection<JournalEntry> list, int totalColumns) {
        int count = 0;
        int sum = 0;
        double lost = 0;
        for (JournalEntry e : list) {
            if (e.getScore() != null && e.getStatus() != CellStatus.ABSENT) {
                count++;
                sum += e.getScore();
            }
            if (e.getStatus() == CellStatus.ABSENT && !e.isExcused()) { lost += 1; }
            else if (e.getStatus() == CellStatus.LATE) { lost += 0.5; }
        }
        if (count == 0) { return "—"; }
        double attendance = totalColumns == 0 ? 1.0 : Math.max(0, 1 - lost / totalColumns);
        double average = (double) sum / count;
        return String.format("%.2f", average * (0.9 + 0.1 * attendance));
    }

    // Проверка на повышенную стипендию по всем предметам студента (правило из ТЗ: средний от 90)
    public ScholarshipResult scholarship(Student student) {
        List<ScholarshipResult.Line> lines = new ArrayList<>();
        StudyGroup group = student.getStudyGroup();
        if (group == null) { return new ScholarshipResult("NO_DATA", lines); }

        boolean noData = false;
        boolean allOk = true;
        for (Subject subject : subjects.findAll()) {
            List<JournalColumn> cols = columns.findByStudyGroupAndSubjectOrderByColumnDateAsc(group, subject);
            if (cols.isEmpty()) { continue; }

            int count = 0;
            int sum = 0;
            for (JournalEntry e : entries.findByStudentAndJournalColumnIn(student, cols)) {
                if (e.getScore() != null && e.getStatus() != CellStatus.ABSENT) {
                    count++;
                    sum += e.getScore();
                }
            }
            if (count == 0) {
                noData = true;
                lines.add(new ScholarshipResult.Line(subject.getName(), "—", 0, 0, false));
                continue;
            }
            double average = (double) sum / count;
            boolean ok = average >= 90;
            if (!ok) { allOk = false; }
            // сколько ещё оценок по 100 нужно, чтобы дотянуть средний до 90
            int need = ok ? 0 : (int) Math.ceil((90.0 * count - sum) / 10.0);
            lines.add(new ScholarshipResult.Line(subject.getName(), String.format("%.2f", average), count, need, ok));
        }
        String status = (lines.isEmpty() || noData) ? "NO_DATA" : (allOk ? "YES" : "NO");
        return new ScholarshipResult(status, lines);
    }
}