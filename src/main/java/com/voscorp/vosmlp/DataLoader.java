package com.voscorp.vosmlp;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.time.LocalDate;

@Component
public class DataLoader implements CommandLineRunner {
    private final StudentRepository students;
    private final AttendanceRepository attendances;

    public DataLoader(StudentRepository students, AttendanceRepository attendances) {
        this.students = students;
        this.attendances = attendances;
    }

    @Override
    public void run(String... args) {
        Student a = students.save(new Student("Иванов Алихан", "24-ИТ-1"));
        Student b = students.save(new Student("Петрова Айгерим", "24-ИТ-1"));
        Student c = students.save(new Student("Смагулов Данияр", "24-ИТ-2"));

        attendances.save(new Attendance(a, "Математика", LocalDate.of(2026, 10, 19), AttendanceStatus.UNEXCUSED));
        attendances.save(new Attendance(b, "Программирование", LocalDate.of(2026, 10, 19), AttendanceStatus.EXCUSED));
        attendances.save(new Attendance(c, "Физика", LocalDate.of(2026, 10, 20), AttendanceStatus.UNEXCUSED));
    }
}