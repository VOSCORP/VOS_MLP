package com.voscorp.vosmlp.journal;

import com.voscorp.vosmlp.auth.AppUser;
import com.voscorp.vosmlp.auth.AppUserRepository;
import com.voscorp.vosmlp.auth.UserRole;
import com.voscorp.vosmlp.notifications.NotificationService;
import com.voscorp.vosmlp.students.Student;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GrantService {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final JournalGrantRepository grants;
    private final AppUserRepository users;
    private final StudyGroupRepository groups;
    private final SubjectRepository subjects;
    private final TeachingAssignmentRepository assignments;
    private final NotificationService notifications;

    public GrantService(JournalGrantRepository grants, AppUserRepository users, StudyGroupRepository groups,
                        SubjectRepository subjects, TeachingAssignmentRepository assignments,
                        NotificationService notifications) {
        this.grants = grants;
        this.users = users;
        this.groups = groups;
        this.subjects = subjects;
        this.assignments = assignments;
        this.notifications = notifications;
    }

    public static String typeLabel(AccessType type) {
        switch (type) {
            case CHANGE: return "Изменить";
            case FILL: return "Заполнить";
            default: return "Изменить и заполнить";
        }
    }

    // ---------- данные для панели преподавателя ----------

    public List<StarostaOption> starostaOptions() {
        List<StarostaOption> list = new ArrayList<>();
        for (AppUser u : users.findByRole(UserRole.STAROSTA)) {
            Student s = u.getLinkedStudent();
            if (s == null || s.getStudyGroup() == null) { continue; }
            list.add(new StarostaOption(u.getId(), u.getFullName(), s.getStudyGroup().getId(), s.getStudyGroup().getName()));
        }
        list.sort(Comparator.comparing(StarostaOption::getGroupName).thenComparing(StarostaOption::getName));
        return list;
    }

    // группы преподавателя и предметы, которые он в них ведёт
    public List<GroupOption> teacherGroups(AppUser teacher) {
        Map<Long, StudyGroup> groupMap = new LinkedHashMap<>();
        Map<Long, List<Subject>> subjectMap = new LinkedHashMap<>();
        for (TeachingAssignment a : assignments.findByTeacher(teacher)) {
            Long gid = a.getStudyGroup().getId();
            groupMap.put(gid, a.getStudyGroup());
            subjectMap.computeIfAbsent(gid, k -> new ArrayList<>()).add(a.getSubject());
        }
        List<GroupOption> list = new ArrayList<>();
        for (StudyGroup g : groupMap.values()) {
            list.add(new GroupOption(g.getId(), g.getName(), subjectMap.get(g.getId())));
        }
        list.sort(Comparator.comparing(GroupOption::getName));
        return list;
    }

    public List<GrantView> viewsIssuedBy(AppUser teacher) {
        List<GrantView> list = new ArrayList<>();
        for (JournalGrant g : grants.findByTeacherOrderByCreatedAtDesc(teacher)) {
            if (list.size() >= 10) { break; }
            list.add(toView(g));
        }
        return list;
    }

    public List<GrantView> viewsForStarosta(AppUser starosta) {
        List<GrantView> list = new ArrayList<>();
        for (JournalGrant g : grants.findByStarosta(starosta)) {
            if (g.isLive()) { list.add(toView(g)); }
        }
        return list;
    }

    private GrantView toView(JournalGrant g) {
        String subjectsText = g.getSubjects().stream().map(Subject::getName).sorted().collect(Collectors.joining(", "));
        String label;
        String state;
        switch (g.getStatus()) {
            case ACTIVE: label = "Действует"; state = "ok"; break;
            case DONE: label = "Староста выполнил"; state = "warn"; break;
            case CLOSED: label = "Закрыт"; state = "bad"; break;
            default: label = "Закрыт автоматически"; state = "bad";
        }
        boolean live = g.isLive();
        return new GrantView(g.getId(), g.getTeacher().getFullName(), g.getStarosta().getFullName(),
                g.getStudyGroup().getName(), subjectsText, typeLabel(g.getAccessType()),
                g.getExpiresAt().format(FMT), label, state, live, live && g.getStatus() == GrantStatus.ACTIVE);
    }

    // ---------- действия ----------

    // Возвращает текст ошибки или null, если доступ выдан
    public String create(AppUser teacher, Long starostaId, String scope, Long targetGroupId,
                         boolean notifyStarosta, boolean notifyCurator, String accessTypeRaw,
                         List<Long> subjectIds, String expiresRaw) {
        if (teacher.getRole() != UserRole.TEACHER) {
            return "Выдавать доступ может только преподаватель";
        }
        AppUser starosta = starostaId == null ? null : users.findById(starostaId).orElse(null);
        if (starosta == null || starosta.getRole() != UserRole.STAROSTA
                || starosta.getLinkedStudent() == null || starosta.getLinkedStudent().getStudyGroup() == null) {
            return "Староста не найден";
        }
        StudyGroup ownGroup = starosta.getLinkedStudent().getStudyGroup();
        boolean other = "other".equals(scope);
        StudyGroup target = other
                ? (targetGroupId == null ? null : groups.findById(targetGroupId).orElse(null))
                : ownGroup;
        if (target == null) { return "Группа не найдена"; }

        AccessType type;
        try {
            type = AccessType.valueOf(accessTypeRaw);
        } catch (Exception ex) {
            return "Выберите тип доступа";
        }

        if (subjectIds == null || subjectIds.isEmpty()) {
            return "Выберите хотя бы один предмет (вы можете выдавать доступ только по предметам, которые ведёте в этой группе)";
        }
        Set<Subject> chosen = new LinkedHashSet<>();
        for (Long sid : subjectIds) {
            Subject s = subjects.findById(sid).orElse(null);
            if (s == null || !assignments.existsByTeacherAndStudyGroupAndSubject(teacher, target, s)) {
                return "Вы можете выдавать доступ только по предметам, которые ведёте в этой группе";
            }
            chosen.add(s);
        }

        LocalDateTime expires;
        try {
            expires = LocalDateTime.parse(expiresRaw);
        } catch (Exception ex) {
            return "Укажите дату и время автоматического закрытия доступа";
        }
        if (!expires.isAfter(LocalDateTime.now())) {
            return "Время автоматического закрытия должно быть в будущем";
        }

        grants.save(new JournalGrant(teacher, starosta, target, chosen, type, expires));

        // уведомления
        String subjectsText = chosen.stream().map(Subject::getName).sorted().collect(Collectors.joining(", "));
        String label = typeLabel(type);
        notifications.send(starosta, "Преподаватель " + teacher.getFullName() + " выдал вам разрешение «" + label
                + "» на управление журналом: группа " + target.getName() + ", предметы: " + subjectsText
                + ". Доступ закроется " + expires.format(FMT) + ".", "/journal");

        if (other) {
            String info = "Преподаватель " + teacher.getFullName() + " выдал разрешение «" + label
                    + "» на управление журналом группы " + target.getName() + " старосте "
                    + starosta.getFullName() + " (" + ownGroup.getName() + ").";
            Set<Long> notified = new HashSet<>();
            notified.add(starosta.getId());
            notified.add(teacher.getId());
            if (notifyStarosta) {
                for (AppUser u : users.findByRole(UserRole.STAROSTA)) {
                    Student s = u.getLinkedStudent();
                    if (s != null && s.getStudyGroup() != null && s.getStudyGroup().getId().equals(target.getId())
                            && notified.add(u.getId())) {
                        notifications.send(u, info, "/journal");
                    }
                }
            }
            if (notifyCurator && target.getCurator() != null && notified.add(target.getCurator().getId())) {
                notifications.send(target.getCurator(), info, "/journal");
            }
        }
        return null;
    }

    // Староста нажал «Выполнил»
    public String markDone(AppUser starosta, Long grantId) {
        JournalGrant g = grants.findById(grantId).orElse(null);
        if (g == null || !g.getStarosta().getId().equals(starosta.getId())) { return "Доступ не найден"; }
        if (g.getStatus() != GrantStatus.ACTIVE || !g.isLive()) { return "Этот доступ уже не действует"; }
        g.setStatus(GrantStatus.DONE);
        grants.save(g);
        notifications.send(g.getTeacher(), "Староста " + starosta.getFullName() + " выполнил работу в журнале (группа "
                + g.getStudyGroup().getName() + "). Закройте доступ.", "/journal");
        return null;
    }

    // Преподаватель (или админ) закрывает доступ
    public String close(AppUser actor, Long grantId) {
        JournalGrant g = grants.findById(grantId).orElse(null);
        if (g == null) { return "Доступ не найден"; }
        boolean allowed = actor.getRole() == UserRole.ADMIN || g.getTeacher().getId().equals(actor.getId());
        if (!allowed) { return "Закрыть доступ может только выдавший его преподаватель"; }
        if (!g.isLive()) { return "Этот доступ уже закрыт"; }
        g.setStatus(GrantStatus.CLOSED);
        grants.save(g);
        notifications.send(g.getStarosta(), "Доступ к журналу (группа " + g.getStudyGroup().getName()
                + ") закрыт преподавателем " + actor.getFullName() + ".", "/journal");
        return null;
    }

    // Автоматическое закрытие: вызывается при открытии журнала
    public void expireOverdue() {
        LocalDateTime now = LocalDateTime.now();
        for (JournalGrant g : grants.findByStatusIn(List.of(GrantStatus.ACTIVE, GrantStatus.DONE))) {
            if (!g.getExpiresAt().isAfter(now)) {
                g.setStatus(GrantStatus.EXPIRED);
                grants.save(g);
                String text = "Доступ старосты " + g.getStarosta().getFullName() + " к журналу (группа "
                        + g.getStudyGroup().getName() + ") закрыт автоматически: истекло время.";
                notifications.send(g.getTeacher(), text, "/journal");
                notifications.send(g.getStarosta(), text, "/journal");
            }
        }
    }
}