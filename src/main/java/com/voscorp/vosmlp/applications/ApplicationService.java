package com.voscorp.vosmlp.applications;

import com.voscorp.vosmlp.attendance.AttendanceMark;
import com.voscorp.vosmlp.attendance.AttendanceRecord;
import com.voscorp.vosmlp.attendance.AttendanceRecordRepository;
import com.voscorp.vosmlp.audit.AuditAction;
import com.voscorp.vosmlp.audit.AuditService;
import com.voscorp.vosmlp.auth.AppUser;
import com.voscorp.vosmlp.auth.UserRole;
import com.voscorp.vosmlp.discipline.*;
import com.voscorp.vosmlp.documents.DocumentService;
import com.voscorp.vosmlp.documents.MedicalResult;
import com.voscorp.vosmlp.documents.UserDocument;
import com.voscorp.vosmlp.journal.JournalColumn;
import com.voscorp.vosmlp.journal.TeachingAssignment;
import com.voscorp.vosmlp.journal.TeachingAssignmentRepository;
import com.voscorp.vosmlp.notifications.NotificationService;
import com.voscorp.vosmlp.students.Student;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ApplicationService {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public record Result(boolean ok, String message) {
        public static Result ok(String m) { return new Result(true, m); }
        public static Result error(String m) { return new Result(false, m); }
    }

    public record CreateResult(Application application, String error) {}

    private final ApplicationRepository apps;
    private final AttendanceChangeRequestRepository details;
    private final AttendanceRecordRepository records;
    private final TeachingAssignmentRepository assignments;
    private final DocumentService documents;
    private final NotificationService notifications;
    private final AuditService audit;
    private final MedicalApiMock medicalApi;
    private final DisciplinaryNoticeRepository notices;
    private final ExpulsionProposalRepository proposals;

    public ApplicationService(ApplicationRepository apps, AttendanceChangeRequestRepository details,
                              AttendanceRecordRepository records, TeachingAssignmentRepository assignments,
                              DocumentService documents, NotificationService notifications, AuditService audit,
                              MedicalApiMock medicalApi, DisciplinaryNoticeRepository notices,
                              ExpulsionProposalRepository proposals) {
        this.apps = apps;
        this.details = details;
        this.records = records;
        this.assignments = assignments;
        this.documents = documents;
        this.notifications = notifications;
        this.audit = audit;
        this.medicalApi = medicalApi;
        this.notices = notices;
        this.proposals = proposals;
    }

    public List<ApplicationType> typesFor(AppUser user) {
        return ApplicationType.availableFor(user.getRole());
    }

    // ---------- списки ----------

    public List<Map<String, Object>> mine(AppUser user) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Application a : apps.findByApplicantOrderByCreatedAtDesc(user)) { list.add(view(a, user, false)); }
        return list;
    }

    public List<Map<String, Object>> inbox(AppUser admin) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Application a : apps.findAllByOrderByCreatedAtDesc()) { list.add(view(a, admin, true)); }
        return list;
    }

    // заявку видит автор и админ
    public Application find(AppUser user, Long id) {
        Application a = id == null ? null : apps.findById(id).orElse(null);
        if (a == null) { return null; }
        boolean ok = a.getApplicant().getId().equals(user.getId()) || user.getRole() == UserRole.ADMIN;
        return ok ? a : null;
    }

    public Map<String, Object> view(Application a, AppUser viewer, boolean adminMode) {
        Map<String, Object> m = new LinkedHashMap<>();
        Student student = a.getApplicant().getLinkedStudent();
        m.put("id", a.getId());
        m.put("number", "№" + a.getId());
        m.put("typeLabel", a.getType().getLabel());
        m.put("applicantName", a.getApplicant().getFullName());
        m.put("groupName", student != null && student.getStudyGroup() != null ? student.getStudyGroup().getName() : "");
        m.put("title", a.getTitle());
        m.put("createdText", a.getCreatedAt().format(DATE_TIME));
        m.put("statusLabel", a.getStatus().getLabel());
        m.put("statusClass", a.getStatus().getCss());
        m.put("details", a.getDetails());
        m.put("comment", a.getComment());
        m.put("signTrace", a.getSignTrace() == null ? "" : a.getSignTrace());
        m.put("decisionNote", a.getDecisionNote() == null ? "" : a.getDecisionNote());
        m.put("decidedBy", a.getDecidedBy() == null ? "" : a.getDecidedBy());

        AttendanceChangeRequest d = a.getType() == ApplicationType.ATTENDANCE_CHANGE
                ? details.findByApplication(a).orElse(null) : null;
        m.put("certificateNumber", d == null ? "" : d.getCertificateNumber());
        m.put("certificateUrl", d == null || d.getCertificateDocumentId() == null
                ? "" : "/documents/" + d.getCertificateDocumentId() + "/download");
        MedicalResult mr = d == null ? null : d.getMedicalResult();
        String medLabel = "Не проверена";
        String medClass = "warn";
        if (mr == MedicalResult.VALID) { medLabel = "Подтверждена (VALID)"; medClass = "ok"; }
        else if (mr == MedicalResult.NOT_FOUND) { medLabel = "Не найдена (NOT_FOUND)"; medClass = "bad"; }
        else if (mr == MedicalResult.DATA_MISMATCH) { medLabel = "Данные не совпадают (DATA_MISMATCH)"; medClass = "bad"; }
        m.put("hasCertificate", d != null);
        m.put("medicalLabel", medLabel);
        m.put("medicalClass", medClass);

        boolean pending = a.getStatus() == ApplicationStatus.PENDING_DECISION;
        boolean owner = a.getApplicant().getId().equals(viewer.getId());
        m.put("canSign", !adminMode && owner && a.getStatus() == ApplicationStatus.PENDING_SIGNATURE
                && a.getType().getSignAction() != null);
        m.put("canVerify", adminMode && pending && d != null && mr == null);
        m.put("canApprove", adminMode && pending && d != null && mr == MedicalResult.VALID);
        m.put("canReject", adminMode && pending);
        return m;
    }

    // ---------- заявка на изменение посещаемости ----------

    private String teachersOf(JournalColumn c) {
        List<String> names = new ArrayList<>();
        for (TeachingAssignment ta : assignments.findByStudyGroupAndSubject(c.getStudyGroup(), c.getSubject())) {
            names.add(ta.getTeacher().getFullName());
        }
        return names.isEmpty() ? "не назначен" : String.join(", ", names);
    }

    private Set<Long> busyRecordIds(AppUser user) {
        Set<Long> busy = new HashSet<>();
        for (Application a : apps.findByApplicantOrderByCreatedAtDesc(user)) {
            if (a.getType() != ApplicationType.ATTENDANCE_CHANGE) { continue; }
            if (a.getStatus() == ApplicationStatus.PENDING_SIGNATURE || a.getStatus() == ApplicationStatus.PENDING_DECISION) {
                details.findByApplication(a).ifPresent(d -> busy.addAll(d.getRecordIds()));
            }
        }
        return busy;
    }

    // пропуски без причины, которые можно включить в заявку
    public List<Map<String, Object>> absencesFor(AppUser user) {
        List<Map<String, Object>> list = new ArrayList<>();
        Student student = user.getLinkedStudent();
        if (student == null) { return list; }
        Set<Long> busy = busyRecordIds(user);
        List<AttendanceRecord> all = new ArrayList<>(records.findByStudent(student));
        all.sort(Comparator.comparing((AttendanceRecord r) -> r.getJournalColumn().getColumnDate()));
        for (AttendanceRecord r : all) {
            if (r.getMark() != AttendanceMark.ABSENT || r.isExcused() || busy.contains(r.getId())) { continue; }
            JournalColumn c = r.getJournalColumn();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("dateText", c.getColumnDate().format(DATE));
            m.put("iso", c.getColumnDate().toString());
            m.put("subject", c.getSubject().getName());
            m.put("teachers", teachersOf(c));
            m.put("markedBy", r.getMarkedBy() == null ? "" : r.getMarkedBy());
            list.add(m);
        }
        return list;
    }

    public CreateResult createAttendanceChange(AppUser user, List<Long> recordIds, String certificateNumber,
                                               String comment, MultipartFile file) {
        if (!ApplicationType.ATTENDANCE_CHANGE.getRoles().contains(user.getRole()) || user.getLinkedStudent() == null) {
            return new CreateResult(null, "Эта заявка доступна студентам и старостам");
        }
        if (recordIds == null || recordIds.isEmpty()) { return new CreateResult(null, "Выберите хотя бы один пропуск"); }
        if (recordIds.size() > 30) { return new CreateResult(null, "Не больше 30 пропусков в одной заявке"); }
        if (certificateNumber == null || certificateNumber.isBlank() || certificateNumber.length() > 40) {
            return new CreateResult(null, "Укажите номер справки (до 40 символов)");
        }
        if (comment == null || comment.isBlank() || comment.length() > 1000) {
            return new CreateResult(null, "Напишите комментарий (до 1000 символов)");
        }
        String fileError = documents.validateUpload(file);
        if (fileError != null) { return new CreateResult(null, fileError); }

        Set<Long> allowed = new HashSet<>();
        for (Map<String, Object> m : absencesFor(user)) { allowed.add((Long) m.get("id")); }
        List<AttendanceRecord> chosen = new ArrayList<>();
        for (Long rid : new LinkedHashSet<>(recordIds)) {
            AttendanceRecord r = allowed.contains(rid) ? records.findById(rid).orElse(null) : null;
            if (r == null) { return new CreateResult(null, "Один из выбранных пропусков нельзя включить в заявку"); }
            chosen.add(r);
        }
        chosen.sort(Comparator.comparing((AttendanceRecord r) -> r.getJournalColumn().getColumnDate()));

        // система сама собирает: предмет, дата, преподаватель
        StringBuilder text = new StringBuilder("Пропусков без причины: " + chosen.size() + "\n");
        for (AttendanceRecord r : chosen) {
            JournalColumn c = r.getJournalColumn();
            text.append(c.getColumnDate().format(DATE)).append(" · ").append(c.getSubject().getName())
                    .append(" · преподаватель: ").append(teachersOf(c)).append("\n");
        }
        String number = certificateNumber.trim();
        String title = "Изменение посещаемости: " + chosen.size() + " пропусков, справка № " + number;

        Application app = apps.save(new Application(ApplicationType.ATTENDANCE_CHANGE, user, title,
                comment.trim(), text.toString().trim()));
        UserDocument cert = documents.saveOtherDocument(user, "Справка № " + number,
                "Заявка на изменение посещаемости №" + app.getId(), file);
        details.save(new AttendanceChangeRequest(app, number, cert.getId(),
                chosen.stream().map(AttendanceRecord::getId).toList()));
        return new CreateResult(app, null);
    }

    // ---------- решения администратора ----------

    private String target(Application a) {
        return "Заявка №" + a.getId() + " · " + a.getApplicant().getFullName();
    }

    public Result verify(AppUser admin, Long id) {
        Application a = id == null ? null : apps.findById(id).orElse(null);
        AttendanceChangeRequest d = a == null ? null : details.findByApplication(a).orElse(null);
        if (a == null || d == null || a.getStatus() != ApplicationStatus.PENDING_DECISION) {
            return Result.error("Заявка не ждёт проверки справки");
        }
        if (d.getMedicalResult() != null) { return Result.error("Справка уже проверена"); }

        MedicalResult result = medicalApi.verify(d.getCertificateNumber());
        d.setMedicalResult(result);
        details.save(d);
        audit.log(admin, AuditAction.DOC_VERIFIED, target(a), null, result.name(),
                "Справка № " + d.getCertificateNumber() + ", проверка через Medical API (имитация)");

        if (result == MedicalResult.VALID) {
            return Result.ok("Справка подтверждена (VALID). Заявку можно одобрить.");
        }
        // старая функция: справка не прошла проверку, значит автоматическое замечание
        Student student = a.getApplicant().getLinkedStudent();
        if (student != null && !notices.existsByApplicationId(a.getId())) {
            notices.save(new DisciplinaryNotice(student, a.getId(), "Справка не прошла проверку: " + result.name()));
            long count = notices.countByStudent(student);
            audit.log(admin, AuditAction.NOTICE_CREATED, student.getFullName(), null, "замечаний: " + count,
                    "Справка не прошла проверку: " + result.name());
            if (count > 3 && !proposals.existsByStudentAndStatus(student, ProposalStatus.PENDING_ADMIN_APPROVAL)) {
                proposals.save(new ExpulsionProposal(student, (int) count));
                audit.log(admin, AuditAction.EXPULSION_PROPOSED, student.getFullName(), null,
                        "ждёт решения администратора", "Замечаний: " + count);
            }
        }
        return Result.ok("Справка не прошла проверку (" + result.name()
                + "). Студенту выдано дисциплинарное замечание. Заявку можно отклонить.");
    }

    public Result approve(AppUser admin, Long id) {
        Application a = id == null ? null : apps.findById(id).orElse(null);
        AttendanceChangeRequest d = a == null ? null : details.findByApplication(a).orElse(null);
        if (a == null || d == null || a.getStatus() != ApplicationStatus.PENDING_DECISION) {
            return Result.error("Заявка уже обработана");
        }
        if (d.getMedicalResult() != MedicalResult.VALID) {
            return Result.error("Одобрить можно только после проверки справки с результатом VALID");
        }

        Map<Long, AppUser> teachers = new LinkedHashMap<>();
        Map<Long, Map<String, List<String>>> lines = new LinkedHashMap<>();
        int count = 0;
        for (Long rid : d.getRecordIds()) {
            AttendanceRecord r = records.findById(rid).orElse(null);
            if (r == null || r.getMark() != AttendanceMark.ABSENT || r.isExcused()) { continue; }
            r.update(AttendanceMark.ABSENT, null, true, admin.getFullName());
            records.save(r);
            count++;
            JournalColumn c = r.getJournalColumn();
            for (TeachingAssignment ta : assignments.findByStudyGroupAndSubject(c.getStudyGroup(), c.getSubject())) {
                AppUser t = ta.getTeacher();
                teachers.put(t.getId(), t);
                lines.computeIfAbsent(t.getId(), k -> new LinkedHashMap<>())
                        .computeIfAbsent(c.getSubject().getName(), k -> new ArrayList<>())
                        .add(c.getColumnDate().format(DATE));
            }
        }
        if (count == 0) { return Result.error("Выбранные пропуски уже изменены"); }

        a.approve(admin.getFullName(), null);
        apps.save(a);
        audit.log(admin, AuditAction.APPLICATION_APPROVED, target(a), "без причины", "уважительная причина",
                "Изменено пропусков: " + count);

        notifications.send(a.getApplicant(), "Заявка №" + a.getId() + " одобрена: пропуски (" + count
                + ") теперь с уважительной причиной.", "/applications/" + a.getId());
        String studentName = a.getApplicant().getFullName();
        for (Map.Entry<Long, Map<String, List<String>>> e : lines.entrySet()) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, List<String>> s : e.getValue().entrySet()) {
                sb.append("«").append(s.getKey()).append("»: ").append(String.join(", ", s.getValue())).append("; ");
            }
            notifications.send(teachers.get(e.getKey()), "Администрация изменила посещаемость студента " + studentName
                    + " по заявке №" + a.getId() + ": " + sb + "пропуски теперь с уважительной причиной.", "/attendance");
        }
        return Result.ok("Заявка одобрена, пропуски изменены, студент и преподаватели уведомлены.");
    }

    public Result reject(AppUser admin, Long id, String note) {
        Application a = id == null ? null : apps.findById(id).orElse(null);
        if (a == null || a.getStatus() != ApplicationStatus.PENDING_DECISION) {
            return Result.error("Заявка уже обработана");
        }
        String reason = note == null ? "" : note.trim();
        if (reason.length() > 500) { return Result.error("Причина слишком длинная (до 500 символов)"); }
        a.reject(admin.getFullName(), reason);
        apps.save(a);
        audit.log(admin, AuditAction.APPLICATION_REJECTED, target(a), "на рассмотрении", "отклонена",
                reason.isEmpty() ? null : reason);
        notifications.send(a.getApplicant(), "Заявка №" + a.getId() + " отклонена."
                + (reason.isEmpty() ? "" : " Причина: " + reason), "/applications/" + a.getId());
        return Result.ok("Заявка отклонена, студент уведомлён.");
    }
}