package com.voscorp.vosmlp.common;

import com.voscorp.vosmlp.attendance.Attendance;
import com.voscorp.vosmlp.attendance.AttendanceRepository;
import com.voscorp.vosmlp.attendance.AttendanceStatus;
import com.voscorp.vosmlp.grades.Grade;
import com.voscorp.vosmlp.grades.GradeRepository;
import com.voscorp.vosmlp.students.Student;
import com.voscorp.vosmlp.students.StudentRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@Order(1)
public class DataLoader implements CommandLineRunner {
    private final StudentRepository students;
    private final AttendanceRepository attendances;
    private final GradeRepository grades;

    public DataLoader(StudentRepository students, AttendanceRepository attendances, GradeRepository grades) {
        this.students = students;
        this.attendances = attendances;
        this.grades = grades;
    }

    @Override
    public void run(String... args) {
        if (students.count() > 0) { return; }
        Student a = students.save(new Student("Иванов Алихан", "24-ИТ-1"));
        Student b = students.save(new Student("Петрова Айгерим", "24-ИТ-1"));
        Student c = students.save(new Student("Смагулов Данияр", "24-ИТ-2"));

        attendances.save(new Attendance(a, "Математика", LocalDate.of(2026, 10, 19), AttendanceStatus.UNEXCUSED));
        attendances.save(new Attendance(a, "Программирование", LocalDate.of(2026, 10, 20), AttendanceStatus.UNEXCUSED));
        attendances.save(new Attendance(a, "История", LocalDate.of(2026, 10, 21), AttendanceStatus.UNEXCUSED));
        attendances.save(new Attendance(a, "Английский язык", LocalDate.of(2026, 10, 21), AttendanceStatus.UNEXCUSED));
        attendances.save(new Attendance(a, "Информатика", LocalDate.of(2026, 10, 22), AttendanceStatus.UNEXCUSED));
        attendances.save(new Attendance(b, "Программирование", LocalDate.of(2026, 10, 19), AttendanceStatus.EXCUSED));
        attendances.save(new Attendance(c, "Физика", LocalDate.of(2026, 10, 20), AttendanceStatus.UNEXCUSED));

        // Иванов: пример из ТЗ, средний балл 83,75
        int[] ivanov = {70, 70, 85, 90, 100, 95, 75, 85};
        for (int score : ivanov) {
            grades.save(new Grade(a, "Математика", "Текущая", score, LocalDate.of(2026, 10, 15)));
        }
        // Петрова: все предметы от 90
        grades.save(new Grade(b, "Математика", "Текущая", 90, LocalDate.of(2026, 10, 15)));
        grades.save(new Grade(b, "Математика", "Рубежный контроль", 100, LocalDate.of(2026, 10, 19)));
        grades.save(new Grade(b, "Программирование", "Текущая", 94, LocalDate.of(2026, 10, 16)));
        // у Смагулова оценок нет: статус «Данных недостаточно»
    }
}