package com.voscorp.vosmlp.attendance;

import com.voscorp.vosmlp.journal.*;
import com.voscorp.vosmlp.students.Student;
import com.voscorp.vosmlp.students.StudentRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(5)
public class AttendanceSeed implements CommandLineRunner {
    private final AttendanceRecordRepository records;
    private final StudentRepository students;
    private final StudyGroupRepository groups;
    private final SubjectRepository subjects;
    private final JournalColumnRepository columns;

    public AttendanceSeed(AttendanceRecordRepository records, StudentRepository students,
                          StudyGroupRepository groups, SubjectRepository subjects, JournalColumnRepository columns) {
        this.records = records;
        this.students = students;
        this.groups = groups;
        this.subjects = subjects;
        this.columns = columns;
    }

    private Student student(String name) {
        return students.findAll().stream().filter(s -> s.getFullName().equals(name)).findFirst().orElse(null);
    }

    private List<JournalColumn> lessons(StudyGroup group, String subjectName) {
        Subject subject = subjects.findAll().stream().filter(s -> s.getName().equals(subjectName)).findFirst().orElse(null);
        if (subject == null) { return List.of(); }
        return columns.findByStudyGroupAndSubjectOrderByColumnDateAsc(group, subject).stream()
                .filter(c -> c.getType() == ColumnType.LESSON).toList();
    }

    private void add(String studentName, JournalColumn column, AttendanceMark mark, Integer minutes, boolean excused) {
        Student s = student(studentName);
        if (s != null) {
            records.save(new AttendanceRecord(s, column, mark, minutes, excused, "Асанов Бауыржан"));
        }
    }

    @Override
    public void run(String... args) {
        if (records.count() > 0) { return; }
        StudyGroup g1 = groups.findAll().stream().filter(g -> g.getName().equals("РПО1-24К")).findFirst().orElse(null);
        if (g1 == null) { return; }

        List<JournalColumn> prog = lessons(g1, "Программирование");
        List<JournalColumn> math = lessons(g1, "Математика");
        if (prog.size() < 3 || math.size() < 2) { return; }

        add("Иванов Алихан", prog.get(0), AttendanceMark.ABSENT, null, false);
        add("Иванов Алихан", prog.get(1), AttendanceMark.ABSENT, null, false);
        add("Иванов Алихан", prog.get(2), AttendanceMark.LATE, 12, false);
        add("Иванов Алихан", math.get(0), AttendanceMark.ABSENT, null, true);

        add("Смагулов Данияр", prog.get(0), AttendanceMark.LATE, 10, false);
        add("Смагулов Данияр", prog.get(1), AttendanceMark.LATE, 5, false);
        add("Смагулов Данияр", prog.get(2), AttendanceMark.LATE, 15, false);

        add("Каримов Тимур", math.get(1), AttendanceMark.ABSENT, null, false);
    }
}