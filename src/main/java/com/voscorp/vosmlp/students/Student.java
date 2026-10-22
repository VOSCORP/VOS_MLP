package com.voscorp.vosmlp.students;

import jakarta.persistence.*;

@Entity
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String fullName;
    private String groupName;

    @Enumerated(EnumType.STRING)
    private StudentStatus status;

    protected Student() {}

    public Student(String fullName, String groupName) {
        this.fullName = fullName;
        this.groupName = groupName;
        this.status = StudentStatus.ACTIVE;
    }

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getGroupName() { return groupName; }
    public StudentStatus getStatus() { return status; }
    public void setStatus(StudentStatus status) { this.status = status; }
}