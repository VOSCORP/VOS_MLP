package com.voscorp.vosmlp.journal;

import com.voscorp.vosmlp.auth.AppUser;
import jakarta.persistence.*;

@Entity
public class StudyGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;

    @ManyToOne
    private AppUser curator; // куратор группы = преподаватель

    protected StudyGroup() {}
    public StudyGroup(String name) { this.name = name; }

    public Long getId() { return id; }
    public String getName() { return name; }
    public AppUser getCurator() { return curator; }
    public void setCurator(AppUser curator) { this.curator = curator; }
}