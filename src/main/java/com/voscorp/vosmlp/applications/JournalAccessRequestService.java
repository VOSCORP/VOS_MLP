package com.voscorp.vosmlp.applications;

import com.voscorp.vosmlp.audit.AuditAction;
import com.voscorp.vosmlp.audit.AuditService;
import com.voscorp.vosmlp.auth.AppUser;
import com.voscorp.vosmlp.auth.AppUserRepository;
import com.voscorp.vosmlp.auth.UserRole;
import com.voscorp.vosmlp.discipline.DisciplinaryNotice;
import com.voscorp.vosmlp.discipline.DisciplinaryNoticeRepository;
import com.voscorp.vosmlp.journal.*;
import com.voscorp.vosmlp.notifications.NotificationService;
import com.voscorp.vosmlp.students.Student;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class JournalAccessRequestService {
    private static final DateTimeFormatter INPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final ApplicationRepository apps;
    private final JournalAccessRequestRepository details;
    private final AppUserRepository users;
    private final StudyGroupRepository groups;
    private final SubjectRepository subjects;
    private final TeachingAssignmentRepository assignments;
    private final GrantService grants;
    private final NotificationService notifications;
    private final AuditService audit;
    private final DisciplinaryNoticeRepository notices;

    public JournalAccessRequestService(ApplicationRepository apps, JournalAccessRequestRepository details,
                                       AppUserRepository users, StudyGroupRepository groups,
                                       SubjectRepository subjects, TeachingAssignmentRepository assignments,
                                       GrantService grants, NotificationService notifications,
                                       AuditService audit, DisciplinaryNoticeRepository notices) {
        this.apps = apps;
        this.details = details;
        this.users = users;
        this.groups = groups;
        this.subjects = subjects;
        this.assignments = assignments;
        this.grants = grants;
        this.notifications = notifications;
        this.audit = audit;
        this.notices = notices;
    }

    private ApplicationService.CreateResult fail(String message) {
        return new ApplicationService.CreateResult(null, message);
    }

    // ---------- данные для формы старосты ----------

    public Map<String, Object> formData(AppUser starosta) {
        Map<String, Object> m = new LinkedHashMap<>();
        List<AppUser> teachers = new ArrayList<>();
        for (AppUser u : users.findByRole(UserRole.TEACHER)) { teachers.add(u); }
        teachers.sort(Comparator.comparing(AppUser::getFullName));

        Map<String, Map<String, Object>> blocks = new LinkedHashMap<>();
        for (TeachingAssignment ta : assignments.findAll()) {
            String key = ta.getTeacher().getId() + ":" + ta.getStudyGroup().getId();
            Map<String, Object> b = blocks.computeIfAbsent(key, k -> {
                Map<String, Object> x = new LinkedHashMap<>();
                x.put("teacherId", ta.getTeacher().getId());
                x.put("groupId", ta.getStudyGroup().getId());
                x.put("subjects", new ArrayList<Subject>());
                return x;
            });
            @SuppressWarnings("unchecked")
            List<Subject> list = (List<Subject>) b.get("subjects");
            list.add(ta.getSubject());
        }
        StudyGroup own = starosta.getLinkedStudent() == null ? null : starosta.getLinkedStudent().getStudyGroup();
        m.put("teachers", teachers);
        m.put("groups", groups.findAll());
        m.put("blocks", new ArrayList<>(blocks.values()));
        m.put("ownGroupId", own == null ? 0L : own.getId());
        return m;
    }

    // ---------- создание заявки ----------

    public ApplicationService.CreateResult create(AppUser user, Long teacherId, String scope, Long targetGroupId,
                                                  List<Long> subjectIds, String accessTypeRaw, String comment) {
        if (user.getRole() != UserRole.STAROSTA || user.getLinkedStudent() == null
                || user.getLinkedStudent().getStudyGroup() == null) {
            return fail("Эта заявка доступна старостам");
        }
        StudyGroup own = user.getLinkedStudent().getStudyGroup();
        AppUser teacher = teacherId == null ? null : users.findById(teacherId).orElse(null);
        if (teacher == null || teacher.getRole() != UserRole.TEACHER) { return fail("Выберите преподавателя"); }

        StudyGroup target = "other".equals(scope)
                ? (targetGroupId == null ? null : groups.findById(targetGroupId).orElse(null))
                : own;
        if (target == null) { return fail("Выберите группу"); }
        boolean ownGroup = target.getId().equals(own.getId());

        AccessType type;
        try {
            type = AccessType.valueOf(accessTypeRaw);
        } catch (Exception e) {
            return fail("Выберите тип доступа");
        }
        if (subjectIds == null || subjectIds.isEmpty()) { return fail("Выберите хотя бы один предмет"); }
        Set<Subject> chosen = new LinkedHashSet<>();
        for (Long sid : subjectIds) {
            Subject s = subjects.findById(sid).orElse(null);
            if (s == null || !assignments.existsByTeacherAndStudyGroupAndSubject(teacher, target, s)) {
                return fail("Этот преподаватель не ведёт выбранный предмет в этой группе");
            }
            chosen.add(s);
        }
        if (comment == null || comment.isBlank() || comment.length() > 1000) {
            return fail("Напишите, зачем нужен доступ (до 1000 символов)");
        }

        String subjectsText = chosen.stream().map(Subject::getName).sorted().collect(Collectors.joining(", "));
        String typeLabel = GrantService.typeLabel(type);
        String text = "Преподаватель: " + teacher.getFullName() + "\n"
                + "Группа: " + target.getName() + (ownGroup ? " (своя)" : " (другая группа)") + "\n"
                + "Предметы: " + subjectsText + "\n"
                + "Что просит: " + typeLabel;
        String title = "Доступ к журналу: " + target.getName() + ", " + subjectsText + " (" + typeLabel + ")";

        Application app = apps.save(new Application(ApplicationType.JOURNAL_ACCESS, user, title,
                comment.trim(), text, teacher));
        details.save(new JournalAccessRequest(app, target, ownGroup, chosen, type));
        return new ApplicationService.CreateResult(app, null);
    }

    // ---------- что показывать на странице заявки ----------

    public void enrich(Map<String, Object> m, Application a, AppUser viewer, boolean adminMode) {
        boolean teacherSide = !adminMode && a.getRecipient() != null && a.getRecipient().getId().equals(viewer.getId());
        m.put("canTeacherDecide", teacherSide && a.getStatus() == ApplicationStatus.PENDING_DECISION);
        m.put("canAdminInvestigate", adminMode && a.getStatus() == ApplicationStatus.FORWARDED);
        m.put("defaultExpires", LocalDateTime.now().plusHours(2).format(INPUT));
    }

    // ---------- решения преподавателя ----------

    private Application forTeacher(AppUser teacher, Long id) {
        Application a = id == null ? null : apps.findById(id).orElse(null);
        if (a == null || a.getType() != ApplicationType.JOURNAL_ACCESS || a.getRecipient() == null
                || !a.getRecipient().getId().equals(teacher.getId())
                || a.getStatus() != ApplicationStatus.PENDING_DECISION) {
            return null;
        }
        return a;
    }

    private String target(Application a) {
        return "Заявка №" + a.getId() + " · " + a.getApplicant().getFullName();
    }

    public ApplicationService.Result approve(AppUser teacher, Long id, String expiresAt) {
        Application a = forTeacher(teacher, id);
        JournalAccessRequest d = a == null ? null : details.findByApplication(a).orElse(null);
        if (a == null || d == null) { return ApplicationService.Result.error("Заявка не ждёт вашего решения"); }

        List<Long> subjectIds = d.getSubjects().stream().map(Subject::getId).toList();
        // доступ выдаёт тот же механизм, что «Помощь с журналом»; при другой группе уведомляются староста и куратор той группы
        String error = grants.create(teacher, a.getApplicant().getId(), d.isOwnGroup() ? "own" : "other",
                d.getTargetGroup().getId(), true, true, d.getAccessType().name(), subjectIds, expiresAt);
        if (error != null) { return ApplicationService.Result.error(error); }

        a.approve(teacher.getFullName(), null);
        apps.save(a);
        audit.log(teacher, AuditAction.APPLICATION_APPROVED, target(a), "на рассмотрении", "одобрена", "Доступ выдан");
        notifications.send(a.getApplicant(), "Заявка №" + a.getId() + " одобрена преподавателем "
                + teacher.getFullName() + ": доступ к журналу выдан.", "/journal");
        return ApplicationService.Result.ok("Заявка одобрена, доступ выдан.");
    }

    public ApplicationService.Result reject(AppUser teacher, Long id, String note) {
        Application a = forTeacher(teacher, id);
        if (a == null) { return ApplicationService.Result.error("Заявка не ждёт вашего решения"); }
        String reason = note == null ? "" : note.trim();
        if (reason.length() > 500) { return ApplicationService.Result.error("Причина слишком длинная (до 500 символов)"); }
        a.reject(teacher.getFullName(), reason);
        apps.save(a);
        audit.log(teacher, AuditAction.APPLICATION_REJECTED, target(a), "на рассмотрении", "отклонена",
                reason.isEmpty() ? null : reason);
        notifications.send(a.getApplicant(), "Заявка №" + a.getId() + " отклонена преподавателем "
                        + teacher.getFullName() + "." + (reason.isEmpty() ? "" : " Причина: " + reason),
                "/applications/" + a.getId());
        return ApplicationService.Result.ok("Заявка отклонена.");
    }

    public ApplicationService.Result forward(AppUser teacher, Long id, String note) {
        Application a = forTeacher(teacher, id);
        if (a == null) { return ApplicationService.Result.error("Заявка не ждёт вашего решения"); }
        String reason = note == null ? "" : note.trim();
        if (reason.isEmpty()) { return ApplicationService.Result.error("Напишите, что вызывает подозрение"); }
        if (reason.length() > 500) { return ApplicationService.Result.error("Текст слишком длинный (до 500 символов)"); }
        a.forward(teacher.getFullName(), reason);
        apps.save(a);
        audit.log(teacher, AuditAction.APPLICATION_FORWARDED, target(a), "на рассмотрении у преподавателя",
                "передана администрации", reason);
        for (AppUser admin : users.findByRole(UserRole.ADMIN)) {
            notifications.send(admin, "Преподаватель " + teacher.getFullName() + " передал заявку №" + a.getId()
                            + " старосты " + a.getApplicant().getFullName() + " на проверку. Подозрение: " + reason,
                    "/admin/applications/" + a.getId());
        }
        return ApplicationService.Result.ok("Заявка передана администрации.");
    }

    // ---------- решения администратора по переданной заявке ----------

    private Application forwarded(Long id) {
        Application a = id == null ? null : apps.findById(id).orElse(null);
        return (a != null && a.getType() == ApplicationType.JOURNAL_ACCESS
                && a.getStatus() == ApplicationStatus.FORWARDED) ? a : null;
    }

    public ApplicationService.Result confirmAbuse(AppUser admin, Long id, String note) {
        Application a = forwarded(id);
        if (a == null) { return ApplicationService.Result.error("Заявка не передана на проверку"); }
        String reason = note == null ? "" : note.trim();
        if (reason.isEmpty()) { return ApplicationService.Result.error("Опишите, что показало расследование"); }
        if (reason.length() > 400) { return ApplicationService.Result.error("Текст слишком длинный (до 400 символов)"); }

        a.reject(admin.getFullName(), "Злоупотребление подтверждено. " + reason);
        apps.save(a);
        audit.log(admin, AuditAction.APPLICATION_REJECTED, target(a), "передана администрации",
                "отклонена: злоупотребление подтверждено", reason);

        Student student = a.getApplicant().getLinkedStudent();
        if (student != null && !notices.existsByApplicationId(a.getId())) {
            notices.save(new DisciplinaryNotice(student, a.getId(), "Злоупотребление доступом к журналу: " + reason));
            long count = notices.countByStudent(student);
            audit.log(admin, AuditAction.NOTICE_CREATED, student.getFullName(), null, "замечаний: " + count,
                    "Злоупотребление доступом к журналу");
            if (count > 3) {
                for (AppUser other : users.findByRole(UserRole.ADMIN)) {
                    notifications.send(other, "У старосты " + student.getFullName() + " больше трёх замечаний ("
                                    + count + "). По правилам его снимают с поста и проводят переголосование.",
                            "/admin");
                }
            }
        }
        notifications.send(a.getApplicant(), "Заявка №" + a.getId() + " отклонена: подтверждено злоупотребление. "
                + "Вам выдано дисциплинарное замечание.", "/applications/" + a.getId());
        if (a.getRecipient() != null) {
            notifications.send(a.getRecipient(), "По заявке №" + a.getId() + " администрация подтвердила злоупотребление. "
                    + "Старосте выдано замечание.", "/applications/" + a.getId());
        }
        return ApplicationService.Result.ok("Злоупотребление подтверждено, старосте выдано замечание.");
    }

    public ApplicationService.Result clear(AppUser admin, Long id, String note) {
        Application a = forwarded(id);
        if (a == null) { return ApplicationService.Result.error("Заявка не передана на проверку"); }
        String reason = note == null ? "" : note.trim();
        if (reason.isEmpty()) { return ApplicationService.Result.error("Напишите вывод проверки"); }
        if (reason.length() > 400) { return ApplicationService.Result.error("Текст слишком длинный (до 400 символов)"); }

        a.returnToTeacher(admin.getFullName(), "Администрация: нарушения нет. " + reason);
        apps.save(a);
        audit.log(admin, AuditAction.APPLICATION_RETURNED, target(a), "передана администрации",
                "возвращена преподавателю", reason);
        if (a.getRecipient() != null) {
            notifications.send(a.getRecipient(), "Администрация проверила заявку №" + a.getId()
                    + ": нарушения нет. Примите решение.", "/applications/" + a.getId());
        }
        return ApplicationService.Result.ok("Нарушения нет, заявка возвращена преподавателю.");
    }
}