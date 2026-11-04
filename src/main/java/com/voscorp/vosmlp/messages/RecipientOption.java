package com.voscorp.vosmlp.messages;

public class RecipientOption {
    private final Long id;
    private final String label;

    public RecipientOption(Long id, String label) {
        this.id = id;
        this.label = label;
    }

    public Long getId() { return id; }
    public String getLabel() { return label; }
}