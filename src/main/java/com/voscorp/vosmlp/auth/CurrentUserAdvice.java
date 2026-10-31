package com.voscorp.vosmlp.auth;

import com.voscorp.vosmlp.notifications.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Map;

@ControllerAdvice
public class CurrentUserAdvice {
    private static final Map<UserRole, String> LABELS = Map.of(
            UserRole.ADMIN, "Администратор",
            UserRole.TEACHER, "Преподаватель",
            UserRole.STUDENT, "Студент",
            UserRole.PARENT, "Родитель",
            UserRole.STAROSTA, "Староста");

    private final AppUserRepository users;
    private final NotificationService notifications;

    public CurrentUserAdvice(AppUserRepository users, NotificationService notifications) {
        this.users = users;
        this.notifications = notifications;
    }

    @ModelAttribute
    public void addCurrentUser(Model model, Authentication authentication) {
        if (authentication == null) { return; }
        users.findByUsername(authentication.getName()).ifPresent(u -> {
            model.addAttribute("currentName", u.getFullName());
            model.addAttribute("currentRole", u.getRole().name());
            model.addAttribute("currentRoleLabel", LABELS.get(u.getRole()));
            model.addAttribute("unreadCount", notifications.unreadCount(u));
        });
    }
}