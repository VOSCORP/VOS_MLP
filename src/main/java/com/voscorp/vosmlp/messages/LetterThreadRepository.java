package com.voscorp.vosmlp.messages;

import com.voscorp.vosmlp.auth.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LetterThreadRepository extends JpaRepository<LetterThread, Long> {
    List<LetterThread> findByInitiatorOrRecipientOrderByUpdatedAtDesc(AppUser initiator, AppUser recipient);
    long countByRecipientAndUnreadForRecipientTrue(AppUser recipient);
    long countByInitiatorAndUnreadForInitiatorTrue(AppUser initiator);
}