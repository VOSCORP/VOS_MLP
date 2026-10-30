package com.voscorp.vosmlp.auth;

import com.voscorp.vosmlp.students.Student;
import jakarta.persistence.*;

@Entity
@Table(name = "app_user")
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String username;

    private String passwordHash;
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_role")
    private UserRole role;

    protected AppUser() {}

    public AppUser(String username, String passwordHash, String fullName, UserRole role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getFullName() { return fullName; }
    public UserRole getRole() { return role; }

    // для студента и старосты это он сам, для родителя это его ребёнок
    @ManyToOne
    private Student linkedStudent;

    public Student getLinkedStudent() { return linkedStudent; }
    public void setLinkedStudent(Student linkedStudent) { this.linkedStudent = linkedStudent; }
}