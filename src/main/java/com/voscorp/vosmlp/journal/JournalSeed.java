package com.voscorp.vosmlp.journal;

import com.voscorp.vosmlp.students.Student;
import com.voscorp.vosmlp.students.StudentRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@Order(2)
public class JournalSeed implements CommandLineRunner {
    private final StudyGroupRepository groups;
    private final SubjectRepository subjects;
    private final JournalColumnRepository columns;
    private final JournalEntryRepository entries;
    private final StudentRepository students;

    public JournalSeed(StudyGroupRepository groups, SubjectRepository subjects,
                       JournalColumnRepository columns, JournalEntryRepository entries,
                       StudentRepository students) {
        this.groups = groups;
        this.subjects = subjects;
        this.columns = columns;
        this.entries = entries;
        this.students = students;
    }

    @Override
    public void run(String... args) {
        if (groups.count() > 0) { return; }

        StudyGroup group = groups.save(new StudyGroup("РПО1-24К"));

        List<Student> list = new ArrayList<>(students.findAll());
        String[] extra = {"Каримов Тимур", "Сейткали Аружан", "Омаров Ерлан", "Жумабаева Динара", "Нурланов Арман"};
        for (String name : extra) {
            list.add(students.save(new Student(name, group.getName())));
        }
        for (Student s : list) {
            s.setStudyGroup(group);
            students.save(s);
        }

        Subject prog = subjects.save(new Subject("Программирование"));
        Subject math = subjects.save(new Subject("Математика"));

        List<JournalColumn> progCols = new ArrayList<>();
        String[] tasks = {"use case", "activity", "sequence", "state"};
        LocalDate[] dates = {LocalDate.of(2026, 9, 4), LocalDate.of(2026, 9, 11),
                LocalDate.of(2026, 9, 18), LocalDate.of(2026, 9, 25)};
        for (int i = 0; i < tasks.length; i++) {
            progCols.add(columns.save(new JournalColumn(group, prog, dates[i], ColumnType.LESSON, tasks[i])));
        }
        progCols.add(columns.save(new JournalColumn(group, prog, LocalDate.of(2026, 10, 19), ColumnType.RK1, "Рубежный контроль 1")));

        List<JournalColumn> mathCols = new ArrayList<>();
        String[] mathTasks = {"пределы", "производная", "интеграл"};
        for (int i = 0; i < mathTasks.length; i++) {
            mathCols.add(columns.save(new JournalColumn(group, math, LocalDate.of(2026, 9, 5 + 7 * i), ColumnType.LESSON, mathTasks[i])));
        }
        mathCols.add(columns.save(new JournalColumn(group, math, LocalDate.of(2026, 10, 20), ColumnType.RK1, "Рубежный контроль 1")));

        fill(list, progCols);
        fill(list, mathCols);
    }

    private void fill(List<Student> list, List<JournalColumn> cols) {
        for (int c = 0; c < cols.size(); c++) {
            for (int s = 0; s < list.size(); s++) {
                if ((s + c) % 9 == 0) { continue; } // пустая ячейка
                int score = 70 + ((s * 7 + c * 13) % 31);
                JournalEntry entry;
                if (s == 2 && c == 1) {
                    entry = new JournalEntry(list.get(s), cols.get(c), 85, CellStatus.LATE, 10, false);
                } else if (s == 3 && c == 2) {
                    entry = new JournalEntry(list.get(s), cols.get(c), null, CellStatus.ABSENT, null, false);
                } else if (s == 1 && c == 0) {
                    entry = new JournalEntry(list.get(s), cols.get(c), null, CellStatus.ABSENT, null, true);
                } else if (s == 4 && c == 3) {
                    entry = new JournalEntry(list.get(s), cols.get(c), 80, CellStatus.MAKEUP, null, false);
                } else {
                    entry = new JournalEntry(list.get(s), cols.get(c), score, CellStatus.NORMAL, null, false);
                }
                entries.save(entry);
            }
        }
    }
}