package com.voscorp.vosmlp.audit;

import com.voscorp.vosmlp.auth.AppUser;
import com.voscorp.vosmlp.auth.AppUserRepository;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuditService {
    private static final Map<String, String> ROLE_LABELS = Map.of(
            "ADMIN", "Администратор",
            "TEACHER", "Преподаватель",
            "STUDENT", "Студент",
            "PARENT", "Родитель",
            "STAROSTA", "Староста");

    private final AuditEventRepository repo;
    private final AppUserRepository users;

    public AuditService(AuditEventRepository repo, AppUserRepository users) {
        this.repo = repo;
        this.users = users;
    }

    private String roleLabel(AppUser actor) {
        return actor == null ? "Система" : ROLE_LABELS.getOrDefault(actor.getRole().name(), actor.getRole().name());
    }

    private String actorName(AppUser actor) {
        return actor == null ? "Система" : actor.getFullName();
    }

    // успешное действие
    public void log(AppUser actor, AuditAction action, String target, String oldValue, String newValue, String note) {
        repo.save(new AuditEvent(actorName(actor), roleLabel(actor), action, target, oldValue, newValue, note, "OK"));
    }

    // отказ (попытка, которая не прошла)
    public void denied(AppUser actor, AuditAction action, String target, String note) {
        repo.save(new AuditEvent(actorName(actor), roleLabel(actor), action, target, null, null, note, "DENIED"));
    }

    // действие системы (без пользователя)
    public void system(AuditAction action, String target, String note) {
        repo.save(new AuditEvent("Система", "Система", action, target, null, null, note, "OK"));
    }

    // вход и неудачный вход
    public void login(String username, boolean success) {
        AppUser user = users.findByUsername(username).orElse(null);
        String name = user != null ? user.getFullName() : username;
        String role = user != null ? roleLabel(user) : "Неизвестно";
        if (success) {
            repo.save(new AuditEvent(name, role, AuditAction.LOGIN, "Логин: " + username, null, null, null, "OK"));
        } else {
            repo.save(new AuditEvent(name, role, AuditAction.LOGIN_FAILED, "Логин: " + username, null, null,
                    "Неверный логин или пароль", "DENIED"));
        }
    }
}