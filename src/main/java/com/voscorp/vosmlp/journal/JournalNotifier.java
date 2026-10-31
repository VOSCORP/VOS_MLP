package com.voscorp.vosmlp.journal;

import com.voscorp.vosmlp.auth.AppUser;
import com.voscorp.vosmlp.auth.AppUserRepository;
import com.voscorp.vosmlp.notifications.NotificationService;
import com.voscorp.vosmlp.students.Student;
import org.springframework.stereotype.Component;

@Component
public class JournalNotifier {
    private final NotificationService notifications;
    private final AppUserRepository users;

    public JournalNotifier(NotificationService notifications, AppUserRepository users) {
        this.notifications = notifications;
        this.users = users;
    }

    // Уведомление всем аккаунтам, привязанным к студенту (сам студент, староста, родитель)
    public void notifyCell(Student student, String subject, JournalEntry entry, String byName) {
        String what;
        switch (entry.getStatus()) {
            case LATE:
                what = "отмечено опоздание на " + entry.getLateMinutes() + " мин";
                break;
            case ABSENT:
                what = "отмечен пропуск " + (entry.isExcused() ? "с причиной" : "без причины");
                break;
            case MAKEUP:
                what = entry.getScore() == null ? "назначена отработка" : "отработка, оценка " + entry.getScore();
                break;
            default:
                what = "новая оценка " + entry.getScore();
        }
        String text = student.getFullName() + ": " + what + " по «" + subject + "» (" + byName + ")";
        for (AppUser u : users.findByLinkedStudent(student)) {
            notifications.send(u, text, "/journal");
        }
    }
}