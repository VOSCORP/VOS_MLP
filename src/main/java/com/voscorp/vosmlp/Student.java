package com.voscorp.vosmlp;

import jakarta.persistence.*;

@Entity
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String fullName;
    private String groupName;

    protected Student() {}

    public Student(String fullName, String groupName) {
        this.fullName = fullName;
        this.groupName = groupName;
    }

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getGroupName() { return groupName; }
}