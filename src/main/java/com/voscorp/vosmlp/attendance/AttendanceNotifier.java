package com.voscorp.vosmlp.attendance;

import com.voscorp.vosmlp.auth.AppUser;
import com.voscorp.vosmlp.auth.AppUserRepository;
import com.voscorp.vosmlp.journal.JournalColumn;
import com.voscorp.vosmlp.notifications.NotificationService;
import com.voscorp.vosmlp.students.Student;
import org.springframework.stereotype.Component;

@Component
public class AttendanceNotifier {
    private final NotificationService notifications;
    private final AppUserRepository users;

    public AttendanceNotifier(NotificationService notifications, AppUserRepository users) {
        this.notifications = notifications;
        this.users = users;
    }

    // уведомление всем аккаунтам, привязанным к студенту (сам студент, староста, родитель)
    public void notifyMark(Student student, JournalColumn column, AttendanceRecord record, String byName) {
        String text = student.getFullName() + ": " + AttendanceRecord.describe(record) + " по «"
                + column.getSubject().getName() + "» (" + byName + ")";
        for (AppUser u : users.findByLinkedStudent(student)) {
            notifications.send(u, text, "/attendance");
        }
    }
}