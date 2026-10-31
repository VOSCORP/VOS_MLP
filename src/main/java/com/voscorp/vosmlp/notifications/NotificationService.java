package com.voscorp.vosmlp.notifications;

import com.voscorp.vosmlp.auth.AppUser;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private final NotificationRepository repo;

    public NotificationService(NotificationRepository repo) {
        this.repo = repo;
    }

    public void send(AppUser recipient, String text, String link) {
        repo.save(new Notification(recipient, text, link));
    }

    public long unreadCount(AppUser user) {
        return repo.countByRecipientAndSeenFalse(user);
    }
}