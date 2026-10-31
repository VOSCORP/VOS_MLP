package com.voscorp.vosmlp.journal;

public class StarostaOption {
    private final Long id;
    private final String name;
    private final Long groupId;
    private final String groupName;

    public StarostaOption(Long id, String name, Long groupId, String groupName) {
        this.id = id;
        this.name = name;
        this.groupId = groupId;
        this.groupName = groupName;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public Long getGroupId() { return groupId; }
    public String getGroupName() { return groupName; }
}