package com.voscorp.vosmlp.journal;

import jakarta.persistence.*;

@Entity
public class Subject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;

    protected Subject() {}
    public Subject(String name) { this.name = name; }

    public Long getId() { return id; }
    public String getName() { return name; }
}