package com.voscorp.vosmlp.messages;

import com.voscorp.vosmlp.auth.AppUser;
import jakarta.persistence.*;

import java.time.LocalDateTime;

// Цепочка писем между двумя участниками. Инициатор null означает «Система».
@Entity
public class LetterThread {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 200)
    private String subject;

    @ManyToOne
    private AppUser initiator;

    @ManyToOne
    private AppUser recipient;

    private boolean requiresReply;
    private boolean answered;
    private boolean unreadForInitiator;
    private boolean unreadForRecipient;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected LetterThread() {}

    public LetterThread(String subject, AppUser initiator, AppUser recipient, boolean requiresReply) {
        this.subject = subject;
        this.initiator = initiator;
        this.recipient = recipient;
        this.requiresReply = requiresReply;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public boolean isInitiator(AppUser u) {
        return initiator != null && initiator.getId().equals(u.getId());
    }

    public boolean isRecipient(AppUser u) {
        return recipient.getId().equals(u.getId());
    }

    public boolean involves(AppUser u) {
        return isInitiator(u) || isRecipient(u);
    }

    // после нового письма: помечаем непрочитанным у другой стороны
    public void afterLetter(AppUser sender) {
        this.updatedAt = LocalDateTime.now();
        if (sender != null && isRecipient(sender)) {
            this.unreadForInitiator = true;
            this.answered = true;
        } else {
            this.unreadForRecipient = true;
        }
    }

    public void markRead(AppUser reader) {
        if (isRecipient(reader)) { this.unreadForRecipient = false; }
        else if (isInitiator(reader)) { this.unreadForInitiator = false; }
    }

    public Long getId() { return id; }
    public String getSubject() { return subject; }
    public AppUser getInitiator() { return initiator; }
    public AppUser getRecipient() { return recipient; }
    public boolean isRequiresReply() { return requiresReply; }
    public boolean isAnswered() { return answered; }
    public boolean isUnreadForInitiator() { return unreadForInitiator; }
    public boolean isUnreadForRecipient() { return unreadForRecipient; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}