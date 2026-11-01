package com.voscorp.vosmlp.schedule;

import jakarta.persistence.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Entity
public class Semester {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name; // «2026–2027 · 1 семестр»
    private LocalDate startDate;
    private LocalDate endDate;

    protected Semester() {}

    public Semester(String name, LocalDate startDate, LocalDate endDate) {
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    // Номер учебной недели. Неделя, в которую попал первый день семестра, считается первой
    // (даже если семестр начался со вторника).
    public int weekNo(LocalDate date) {
        LocalDate firstMonday = startDate.with(DayOfWeek.MONDAY);
        long days = ChronoUnit.DAYS.between(firstMonday, date);
        return (int) (days / 7) + 1;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
}