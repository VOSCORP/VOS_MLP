package com.voscorp.vosmlp.journal;

import com.voscorp.vosmlp.auth.AppUser;
import com.voscorp.vosmlp.auth.AppUserRepository;
import com.voscorp.vosmlp.auth.UserRole;
import com.voscorp.vosmlp.students.Student;
import com.voscorp.vosmlp.students.StudentRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@Order(4)
public class AccessSeed implements CommandLineRunner {
    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final StudentRepository students;
    private final StudyGroupRepository groups;
    private final SubjectRepository subjects;
    private final JournalColumnRepository columns;
    private final JournalEntryRepository entries;
    private final TeachingAssignmentRepository assignments;

    public AccessSeed(AppUserRepository users, PasswordEncoder encoder, StudentRepository students,
                      StudyGroupRepository groups, SubjectRepository subjects,
                      JournalColumnRepository columns, JournalEntryRepository entries,
                      TeachingAssignmentRepository assignments) {
        this.users = users;
        this.encoder = encoder;
        this.students = students;
        this.groups = groups;
        this.subjects = subjects;
        this.columns = columns;
        this.entries = entries;
        this.assignments = assignments;
    }

    private Student studentByName(String name) {
        return students.findAll().stream().filter(s -> s.getFullName().equals(name)).findFirst().orElseThrow();
    }

    private StudyGroup groupByName(String name) {
        return groups.findAll().stream().filter(g -> g.getName().equals(name)).findFirst().orElseThrow();
    }

    private Subject subjectByName(String name) {
        return subjects.findAll().stream().filter(s -> s.getName().equals(name)).findFirst().orElseThrow();
    }

    @Override
    public void run(String... args) {
        if (assignments.count() > 0) { return; }

        AppUser teacher1 = users.findByUsername("teacher1").orElseThrow();
        AppUser teacher2 = users.save(new AppUser("teacher2", encoder.encode("demo123"), "Касымова Алия", UserRole.TEACHER));

        StudyGroup g1 = groupByName("РПО1-24К");
        Subject prog = subjectByName("Программирование");
        Subject math = subjectByName("Математика");

        // вторая группа с четырьмя студентами
        StudyGroup g2 = groups.save(new StudyGroup("ВТ2-24Р"));
        String[] names = {"Ахметов Расул", "Белова Кристина", "Давлетов Санжар", "Егорова Милана"};
        List<Student> list2 = new ArrayList<>();
        for (String n : names) {
            Student s = new Student(n, g2.getName());
            s.setStudyGroup(g2);
            list2.add(students.save(s));
        }
        LocalDate[] dates = {LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 21)};
        String[] tasks = {"класс", "наследование", "интерфейс"};
        List<JournalColumn> cols2 = new ArrayList<>();
        for (int i = 0; i < dates.length; i++) {
            cols2.add(columns.save(new JournalColumn(g2, prog, dates[i], ColumnType.LESSON, tasks[i])));
        }
        for (int c = 0; c < cols2.size(); c++) {
            for (int s = 0; s < list2.size(); s++) {
                int score = 75 + ((s * 5 + c * 7) % 26);
                entries.save(new JournalEntry(list2.get(s), cols2.get(c), score, CellStatus.NORMAL, null, false));
            }
        }

        // кураторы групп
        g1.setCurator(teacher1);
        groups.save(g1);
        g2.setCurator(teacher2);
        groups.save(g2);

        // кто что ведёт
        assignments.save(new TeachingAssignment(teacher1, g1, prog));
        assignments.save(new TeachingAssignment(teacher1, g2, prog));
        assignments.save(new TeachingAssignment(teacher2, g1, math));

        // привязка демо-аккаунтов к студентам
        Student ivanov = studentByName("Иванов Алихан");
        Student omarov = studentByName("Омаров Ерлан");
        AppUser student1 = users.findByUsername("student1").orElseThrow();
        student1.setLinkedStudent(ivanov);
        users.save(student1);
        AppUser parent1 = users.findByUsername("parent1").orElseThrow();
        parent1.setLinkedStudent(ivanov);
        users.save(parent1);
        AppUser starosta1 = users.findByUsername("starosta1").orElseThrow();
        starosta1.setLinkedStudent(omarov);
        users.save(starosta1);
    }
}