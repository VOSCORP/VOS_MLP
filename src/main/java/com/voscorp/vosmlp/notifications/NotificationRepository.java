package com.voscorp.vosmlp.notifications;

import com.voscorp.vosmlp.auth.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientOrderByCreatedAtDesc(AppUser recipient);
    List<Notification> findByRecipientAndSeenFalse(AppUser recipient);
    List<Notification> findByRecipientAndToastedFalse(AppUser recipient);
    long countByRecipientAndSeenFalse(AppUser recipient);
}