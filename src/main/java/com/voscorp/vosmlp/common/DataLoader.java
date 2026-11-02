package com.voscorp.vosmlp.common;

import com.voscorp.vosmlp.students.Student;
import com.voscorp.vosmlp.students.StudentRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class DataLoader implements CommandLineRunner {
    private final StudentRepository students;

    public DataLoader(StudentRepository students) {
        this.students = students;
    }

    @Override
    public void run(String... args) {
        if (students.count() > 0) { return; }
        students.save(new Student("Иванов Алихан", "24-ИТ-1"));
        students.save(new Student("Петрова Айгерим", "24-ИТ-1"));
        students.save(new Student("Смагулов Данияр", "24-ИТ-2"));
    }
}