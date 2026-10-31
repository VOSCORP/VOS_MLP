package com.voscorp.vosmlp.journal;

import java.util.List;

public class GroupOption {
    private final Long id;
    private final String name;
    private final List<Subject> subjects;

    public GroupOption(Long id, String name, List<Subject> subjects) {
        this.id = id;
        this.name = name;
        this.subjects = subjects;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public List<Subject> getSubjects() { return subjects; }
}