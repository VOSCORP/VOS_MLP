package com.voscorp.vosmlp.journal;

import jakarta.persistence.*;

@Entity
public class StudyGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;

    protected StudyGroup() {}
    public StudyGroup(String name) { this.name = name; }

    public Long getId() { return id; }
    public String getName() { return name; }
}