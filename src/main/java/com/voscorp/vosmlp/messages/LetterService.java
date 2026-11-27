package com.voscorp.vosmlp.messages;

import com.voscorp.vosmlp.audit.AuditAction;
import com.voscorp.vosmlp.audit.AuditService;
import com.voscorp.vosmlp.auth.AppUser;
import com.voscorp.vosmlp.auth.AppUserRepository;
import com.voscorp.vosmlp.auth.UserRole;
import com.voscorp.vosmlp.journal.JournalAccess;
import com.voscorp.vosmlp.journal.StudyGroup;
import com.voscorp.vosmlp.notifications.NotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class LetterService {
    private static final Map<UserRole, String> ROLE_LABELS = Map.of(
            UserRole.ADMIN, "Администратор",
            UserRole.TEACHER, "Преподаватель",
            UserRole.STUDENT, "Студент",
            UserRole.PARENT, "Родитель",
            UserRole.STAROSTA, "Староста");

    private final LetterThreadRepository threads;
    private final LetterRepository letters;
    private final AppUserRepository users;
    private final NotificationService notifications;
    private final AuditService audit;
    private final JournalAccess journalAccess;
    private final int importantPriority;

    public LetterService(LetterThreadRepository threads, LetterRepository letters, AppUserRepository users,
                         NotificationService notifications, AuditService audit, JournalAccess journalAccess,
                         @Value("${vos.messages.important-priority:8}") int importantPriority) {
        this.threads = threads;
        this.letters = letters;
        this.users = users;
        this.notifications = notifications;
        this.audit = audit;
        this.journalAccess = journalAccess;
        this.importantPriority = importantPriority;
    }

    // ---------- права ----------

    public boolean canCompose(AppUser user) {
        return user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.TEACHER;
    }

    // студент и родитель могут писать только запрос на снятие старосты
    public boolean canRequestRemoval(AppUser user) {
        return user.getRole() == UserRole.STUDENT || user.getRole() == UserRole.PARENT;
    }

    public int maxPriority(AppUser user) {
        return canCompose(user) ? 10 : 7;
    }

    public List<RecipientOption> recipientsFor(AppUser user) {
        List<RecipientOption> result = new ArrayList<>();
        if (user.getRole() == UserRole.ADMIN) {
            for (AppUser u : users.findAll()) {
                if (!u.getId().equals(user.getId())) { result.add(option(u)); }
            }
        } else if (user.getRole() == UserRole.TEACHER) {
            Set<Long> groupIds = new HashSet<>();
            for (StudyGroup g : journalAccess.visibleGroups(user)) { groupIds.add(g.getId()); }
            for (AppUser u : users.findAll()) {
                if (u.getId().equals(user.getId())) { continue; }
                boolean ok = u.getRole() == UserRole.ADMIN
                        || (u.getLinkedStudent() != null && u.getLinkedStudent().getStudyGroup() != null
                        && groupIds.contains(u.getLinkedStudent().getStudyGroup().getId())
                        && (u.getRole() == UserRole.STUDENT || u.getRole() == UserRole.STAROSTA
                        || u.getRole() == UserRole.PARENT));
                if (ok) { result.add(option(u)); }
            }
        }
        result.sort(Comparator.comparing(RecipientOption::getLabel));
        return result;
    }

    private RecipientOption option(AppUser u) {
        return new RecipientOption(u.getId(), u.getFullName() + " · " + ROLE_LABELS.get(u.getRole()));
    }

    // Преподаватель получает важное письмо без всплывающего окна: оно видно на его главной странице
    private void notifyRecipient(AppUser recipient, String text, String link, int priority) {
        if (recipient.getRole() == UserRole.TEACHER && priority >= importantPriority) {
            notifications.sendQuiet(recipient, text, link);
        } else {
            notifications.send(recipient, text, link);
        }
    }

    // ---------- отправка ----------

    // Новое письмо от админа или преподавателя. Возвращает текст ошибки или null.
    public String send(AppUser sender, Long recipientId, String subject, String body,
                       String categoryRaw, int priority) {
        if (!canCompose(sender)) { return "Начинать новые письма могут администраторы и преподаватели. Вы можете отвечать на письма."; }
        LetterCategory category;
        try {
            category = LetterCategory.valueOf(categoryRaw);
        } catch (Exception e) {
            return "Выберите категорию письма";
        }
        if (!LetterCategory.forStaff().contains(category)) { return "Эта категория недоступна"; }
        if (priority < 1 || priority > maxPriority(sender)) { return "Приоритет должен быть от 1 до " + maxPriority(sender); }
        if (subject == null || subject.isBlank()) { return "Укажите тему письма"; }
        if (subject.length() > 200) { return "Тема слишком длинная (до 200 символов)"; }
        if (body == null || body.isBlank()) { return "Напишите текст письма"; }
        if (body.length() > 2000) { return "Письмо слишком длинное (до 2000 символов)"; }

        boolean allowed = recipientsFor(sender).stream().anyMatch(o -> o.getId().equals(recipientId));
        AppUser recipient = recipientId == null ? null : users.findById(recipientId).orElse(null);
        if (!allowed || recipient == null) { return "Вы не можете написать этому получателю"; }

        LetterThread thread = new LetterThread(subject.trim(), sender, recipient, true, category, priority);
        thread.afterLetter(sender);
        thread = threads.save(thread);
        letters.save(new Letter(thread, sender, sender.getFullName(), body.trim()));

        notifyRecipient(recipient, "Новое письмо от " + sender.getFullName() + " (приоритет " + priority + "): «"
                + thread.getSubject() + "». Требуется ответ.", "/messages/" + thread.getId(), priority);
        audit.log(sender, AuditAction.LETTER_SENT, "Письмо для " + recipient.getFullName(), null,
                thread.getSubject(), category.getLabel() + ", приоритет " + priority);
        return null;
    }

    // Данные для формы «Запрос на снятие старосты»: группа, куратор, староста (или ошибка)
    public Map<String, String> removalInfo(AppUser user) {
        Map<String, String> info = new HashMap<>();
        if (user.getLinkedStudent() == null || user.getLinkedStudent().getStudyGroup() == null) {
            info.put("error", "Не найдена ваша группа");
            return info;
        }
        StudyGroup group = user.getLinkedStudent().getStudyGroup();
        if (group.getCurator() == null) {
            info.put("error", "У вашей группы нет куратора");
            return info;
        }
        AppUser starosta = users.findByRole(UserRole.STAROSTA).stream()
                .filter(u -> u.getLinkedStudent() != null && u.getLinkedStudent().getStudyGroup() != null
                        && u.getLinkedStudent().getStudyGroup().getId().equals(group.getId()))
                .findFirst().orElse(null);
        if (starosta == null) {
            info.put("error", "В вашей группе сейчас нет старосты");
            return info;
        }
        info.put("group", group.getName());
        info.put("curator", group.getCurator().getFullName());
        info.put("starosta", starosta.getFullName());
        return info;
    }

    // Запрос от студента или родителя куратору: категория и приоритет фиксированы (8)
    public String sendRemovalRequest(AppUser sender, String body) {
        if (!canRequestRemoval(sender)) { return "Эта возможность доступна студентам и родителям"; }
        Map<String, String> info = removalInfo(sender);
        if (info.containsKey("error")) { return info.get("error"); }
        if (body == null || body.isBlank()) { return "Опишите, почему вы просите снять старосту"; }
        if (body.length() > 2000) { return "Письмо слишком длинное (до 2000 символов)"; }

        AppUser curator = sender.getLinkedStudent().getStudyGroup().getCurator();
        LetterCategory category = LetterCategory.STAROSTA_REMOVAL;
        int priority = category.getDefaultPriority();

        LetterThread thread = new LetterThread("Запрос на снятие старосты: " + info.get("starosta"),
                sender, curator, true, category, priority);
        thread.afterLetter(sender);
        thread = threads.save(thread);
        letters.save(new Letter(thread, sender, sender.getFullName(), body.trim()));

        notifyRecipient(curator, "Запрос на снятие старосты " + info.get("starosta") + " от "
                        + sender.getFullName() + " (приоритет " + priority + "). Требуется ответ.",
                "/messages/" + thread.getId(), priority);
        audit.log(sender, AuditAction.LETTER_SENT, "Письмо куратору " + curator.getFullName(), null,
                thread.getSubject(), category.getLabel() + ", приоритет " + priority);
        return null;
    }

    // Письмо от системы (ответа не требует)
    public void sendFromSystem(AppUser recipient, String subject, String body) {
        LetterThread thread = new LetterThread(subject, null, recipient, false, LetterCategory.INFO, 2);
        thread.afterLetter(null);
        thread = threads.save(thread);
        letters.save(new Letter(thread, null, "Система", body));
        notifications.send(recipient, "Новое письмо от системы: «" + subject + "».", "/messages/" + thread.getId());
    }

    // Ответ в цепочке. Возвращает текст ошибки или null.
    public String reply(AppUser sender, Long threadId, String body) {
        LetterThread thread = threadFor(sender, threadId);
        if (thread == null) { return "Письмо не найдено"; }
        if (thread.getInitiator() == null) { return "На письма от системы отвечать не нужно"; }
        if (body == null || body.isBlank()) { return "Напишите текст ответа"; }
        if (body.length() > 2000) { return "Ответ слишком длинный (до 2000 символов)"; }

        letters.save(new Letter(thread, sender, sender.getFullName(), body.trim()));
        thread.afterLetter(sender);
        threads.save(thread);

        AppUser other = thread.isRecipient(sender) ? thread.getInitiator() : thread.getRecipient();
        notifyRecipient(other, sender.getFullName() + " ответил(а) на письмо «" + thread.getSubject() + "».",
                "/messages/" + thread.getId(), thread.getPriority());
        return null;
    }

    // ---------- чтение ----------

    public LetterThread threadFor(AppUser user, Long id) {
        LetterThread t = id == null ? null : threads.findById(id).orElse(null);
        return (t != null && t.involves(user)) ? t : null;
    }

    public void markRead(LetterThread thread, AppUser user) {
        thread.markRead(user);
        threads.save(thread);
    }

    public long unreadCount(AppUser user) {
        return threads.countByRecipientAndUnreadForRecipientTrue(user)
                + threads.countByInitiatorAndUnreadForInitiatorTrue(user);
    }

    public List<ThreadView> listFor(AppUser user) {
        List<ThreadView> result = new ArrayList<>();
        for (LetterThread t : threads.findByInitiatorOrRecipientOrderByUpdatedAtDesc(user, user)) {
            result.add(viewOf(t, user));
        }
        return result;
    }

    // важные письма без ответа для главной страницы преподавателя
    public List<ThreadView> importantFor(AppUser user) {
        List<ThreadView> result = new ArrayList<>();
        for (LetterThread t : threads
                .findByRecipientAndPriorityGreaterThanEqualAndRequiresReplyTrueAndAnsweredFalseOrderByUpdatedAtDesc(
                        user, importantPriority)) {
            result.add(viewOf(t, user));
        }
        return result;
    }

    public ThreadView viewOf(LetterThread t, AppUser me) {
        boolean iAmRecipient = t.isRecipient(me);
        String otherName;
        String otherRole;
        if (iAmRecipient) {
            if (t.getInitiator() == null) {
                otherName = "Система";
                otherRole = "Автоматическое письмо";
            } else {
                otherName = t.getInitiator().getFullName();
                otherRole = ROLE_LABELS.get(t.getInitiator().getRole());
            }
        } else {
            otherName = t.getRecipient().getFullName();
            otherRole = ROLE_LABELS.get(t.getRecipient().getRole());
        }
        boolean unread = iAmRecipient ? t.isUnreadForRecipient() : t.isUnreadForInitiator();

        String label = "";
        String css = "";
        if (t.isRequiresReply()) {
            if (!t.isAnswered()) {
                label = iAmRecipient ? "Требует ответа" : "Ждёт ответа";
                css = iAmRecipient ? "bad" : "warn";
            } else {
                label = "Отвечено";
                css = "ok";
            }
        }
        return new ThreadView(t.getId(), t.getSubject(), otherName, otherRole,
                t.getUpdatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")), unread, label, css,
                t.getCategory().getLabel(), t.getPriority());
    }

    public List<LetterView> lettersOf(LetterThread thread, AppUser me) {
        List<LetterView> result = new ArrayList<>();
        for (Letter l : letters.findByThreadOrderByCreatedAtAsc(thread)) {
            boolean mine = l.getSender() != null && l.getSender().getId().equals(me.getId());
            result.add(new LetterView(l.getSenderName(), l.getBody(), l.getCreatedAtText(), mine));
        }
        return result;
    }
}