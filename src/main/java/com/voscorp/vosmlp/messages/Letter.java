package com.voscorp.vosmlp.messages;

import com.voscorp.vosmlp.auth.AppUser;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
public class Letter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private LetterThread thread;

    @ManyToOne
    private AppUser sender; // null = система

    private String senderName;

    @Column(length = 2000)
    private String body;

    private LocalDateTime createdAt;

    protected Letter() {}

    public Letter(LetterThread thread, AppUser sender, String senderName, String body) {
        this.thread = thread;
        this.sender = sender;
        this.senderName = senderName;
        this.body = body;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public LetterThread getThread() { return thread; }
    public AppUser getSender() { return sender; }
    public String getSenderName() { return senderName; }
    public String getBody() { return body; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public String getCreatedAtText() {
        return createdAt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }
}