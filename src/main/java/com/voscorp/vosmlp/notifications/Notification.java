package com.voscorp.vosmlp.notifications;

import com.voscorp.vosmlp.auth.AppUser;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private AppUser recipient;

    @Column(length = 500)
    private String text;

    private String link;
    private LocalDateTime createdAt;
    private boolean seen;     // прочитано на странице уведомлений
    private boolean toasted;  // всплывающее окно уже показано

    protected Notification() {}

    public Notification(AppUser recipient, String text, String link) {
        this.recipient = recipient;
        this.text = text;
        this.link = link;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public AppUser getRecipient() { return recipient; }
    public String getText() { return text; }
    public String getLink() { return link; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public boolean isSeen() { return seen; }
    public boolean isToasted() { return toasted; }
    public void markSeen() { this.seen = true; }
    public void markToasted() { this.toasted = true; }

    public String getCreatedAtText() {
        return createdAt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }
}