package com.voscorp.vosmlp.attendance;

import com.voscorp.vosmlp.audit.AuditAction;
import com.voscorp.vosmlp.audit.AuditService;
import com.voscorp.vosmlp.auth.AppUser;
import com.voscorp.vosmlp.auth.AppUserRepository;
import com.voscorp.vosmlp.auth.UserRole;
import com.voscorp.vosmlp.journal.*;
import com.voscorp.vosmlp.students.Student;
import com.voscorp.vosmlp.students.StudentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
public class AttendanceController {
    private final AttendanceAccess access;
    private final AttendanceCalc calc;
    private final AttendanceRecordRepository records;
    private final AttendanceNotifier notifier;
    private final JournalColumnRepository columns;
    private final StudentRepository students;
    private final AppUserRepository users;
    private final AuditService audit;
    private final AttendanceAlertService alertService;

    public AttendanceController(AttendanceAccess access, AttendanceCalc calc, AttendanceRecordRepository records,
                                AttendanceNotifier notifier, JournalColumnRepository columns,
                                StudentRepository students, AppUserRepository users, AuditService audit,
                                AttendanceAlertService alertService) {
        this.access = access;
        this.calc = calc;
        this.records = records;
        this.notifier = notifier;
        this.columns = columns;
        this.students = students;
        this.users = users;
        this.audit = audit;
        this.alertService = alertService;
    }

    private AppUser currentUser(Authentication authentication) {
        return users.findByUsername(authentication.getName()).orElseThrow();
    }

    private List<JournalColumn> lessons(StudyGroup group, Subject subject) {
        return columns.findByStudyGroupAndSubjectOrderByColumnDateAsc(group, subject).stream()
                .filter(c -> c.getType() == ColumnType.LESSON)
                .toList();
    }

    @GetMapping("/attendance")
    public String page(@RequestParam(required = false) Long groupId,
                       @RequestParam(required = false) Long subjectId,
                       Authentication authentication,
                       Model model) {
        AppUser user = currentUser(authentication);

        List<StudyGroup> allGroups = access.visibleGroups(user);
        if (allGroups.isEmpty()) {
            model.addAttribute("empty", true);
            return "attendance/attendance";
        }
        StudyGroup group = allGroups.stream()
                .filter(g -> g.getId().equals(groupId)).findFirst().orElse(allGroups.get(0));

        List<Subject> allSubjects = access.visibleSubjects(user, group);
        if (allSubjects.isEmpty()) {
            model.addAttribute("empty", true);
            return "attendance/attendance";
        }
        Subject subject = allSubjects.stream()
                .filter(s -> s.getId().equals(subjectId)).findFirst().orElse(allSubjects.get(0));

        List<JournalColumn> cols = lessons(group, subject);
        List<Student> groupStudents = students.findAll().stream()
                .filter(s -> s.getStudyGroup() != null && s.getStudyGroup().getId().equals(group.getId()))
                .filter(s -> user.getRole() != UserRole.PARENT
                        || (user.getLinkedStudent() != null && user.getLinkedStudent().getId().equals(s.getId())))
                .sorted(Comparator.comparing(Student::getFullName))
                .toList();

        Map<String, AttendanceRecord> byKey = new HashMap<>();
        if (!cols.isEmpty()) {
            for (AttendanceRecord r : records.findByJournalColumnIn(cols)) {
                byKey.put(r.getStudent().getId() + ":" + r.getJournalColumn().getId(), r);
            }
        }

        // статистика считается за текущий период (между ключевыми датами)
        LocalDate[] period = calc.currentPeriod(group);
        Set<Long> periodIds = new HashSet<>();
        for (JournalColumn c : cols) {
            if (calc.inPeriod(c, period)) { periodIds.add(c.getId()); }
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM");
        List<ColumnView> columnViews = new ArrayList<>();
        for (JournalColumn c : cols) {
            columnViews.add(new ColumnView(c.getColumnDate().format(fmt), "", ""));
        }

        List<AttendanceRow> rows = new ArrayList<>();
        for (Student s : groupStudents) {
            List<AttendanceCellView> cells = new ArrayList<>();
            List<AttendanceRecord> inPeriod = new ArrayList<>();
            for (JournalColumn c : cols) {
                AttendanceRecord r = byKey.get(s.getId() + ":" + c.getId());
                cells.add(new AttendanceCellView(s.getId(), c.getId(), r));
                if (r != null && periodIds.contains(c.getId())) { inPeriod.add(r); }
            }
            int[] st = calc.stats(inPeriod, periodIds.size());
            rows.add(new AttendanceRow(s.getId(), s.getFullName(), cells, st[0], st[1], st[2]));
        }

        model.addAttribute("groups", allGroups);
        model.addAttribute("subjects", allSubjects);
        model.addAttribute("selectedGroupId", group.getId());
        model.addAttribute("selectedSubjectId", subject.getId());
        model.addAttribute("editable", access.canMarkAny(user, group, subject));
        model.addAttribute("columnViews", columnViews);
        model.addAttribute("rows", rows);
        return "attendance/attendance";
    }

    // Отметка посещаемости (вызывается из JavaScript)
    @PostMapping("/attendance/cell")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> mark(@RequestParam Long studentId,
                                                    @RequestParam Long columnId,
                                                    @RequestParam String state,
                                                    @RequestParam(required = false) Integer minutes,
                                                    @RequestParam(defaultValue = "false") boolean excused,
                                                    Authentication authentication) {
        AppUser user = currentUser(authentication);
        Student student = students.findById(studentId).orElse(null);
        JournalColumn column = columns.findById(columnId).orElse(null);
        if (student == null || column == null) {
            return error(HttpStatus.BAD_REQUEST, "Студент или занятие не найдены");
        }
        String target = student.getFullName() + " · " + column.getSubject().getName() + " · "
                + column.getStudyGroup().getName() + " · "
                + column.getColumnDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

        // проверка прав на сервере
        String denied = access.checkMark(user, column);
        if (denied != null) {
            audit.denied(user, AuditAction.ACCESS_DENIED, target, "Попытка отметить посещаемость. Причина отказа: " + denied);
            return error(HttpStatus.FORBIDDEN, denied);
        }
        if (student.getStudyGroup() == null || !student.getStudyGroup().getId().equals(column.getStudyGroup().getId())) {
            return error(HttpStatus.BAD_REQUEST, "Студент не из этой группы");
        }

        AttendanceRecord existing = records.findByStudentAndJournalColumn(student, column).orElse(null);
        String oldText = AttendanceRecord.describe(existing);
        AttendanceRecord saved = null;

        if ("PRESENT".equals(state)) {
            if (existing != null) { records.delete(existing); }
        } else if ("LATE".equals(state) || "ABSENT".equals(state)) {
            AttendanceMark mark = AttendanceMark.valueOf(state);
            Integer lateMinutes = null;
            if (mark == AttendanceMark.LATE) {
                if (minutes == null || minutes < 1 || minutes > 80) {
                    return error(HttpStatus.BAD_REQUEST, "Укажите опоздание в минутах (от 1 до 80)");
                }
                lateMinutes = minutes;
            }
            boolean ex = mark == AttendanceMark.ABSENT && excused;
            if (existing == null) {
                saved = records.save(new AttendanceRecord(student, column, mark, lateMinutes, ex, user.getFullName()));
            } else {
                existing.update(mark, lateMinutes, ex, user.getFullName());
                saved = records.save(existing);
            }
        } else {
            return error(HttpStatus.BAD_REQUEST, "Неизвестная отметка");
        }

        String newText = AttendanceRecord.describe(saved);
        if (!oldText.equals(newText)) {
            audit.log(user, AuditAction.ATTENDANCE_MARK, target, oldText, newText, null);
            notifier.notifyMark(student, column, saved, user.getFullName());
            alertService.check(student); // проверка порогов: при срабатывании админы получат уведомление
        }

        // обновлённая статистика строки (за текущий период)
        StudyGroup group = column.getStudyGroup();
        LocalDate[] period = calc.currentPeriod(group);
        List<JournalColumn> periodCols = lessons(group, column.getSubject()).stream()
                .filter(c -> calc.inPeriod(c, period)).toList();
        int[] st = calc.stats(periodCols.isEmpty() ? List.of()
                : records.findByStudentAndJournalColumnIn(student, periodCols), periodCols.size());

        AttendanceCellView cell = new AttendanceCellView(studentId, columnId, saved);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", true);
        body.put("text", cell.getText());
        body.put("css", cell.getCss());
        body.put("title", cell.getTitle());
        body.put("state", cell.getState());
        body.put("minutes", cell.getMinutes());
        body.put("excused", cell.isExcused());
        body.put("absences", st[0]);
        body.put("lates", st[1]);
        body.put("percent", st[2]);
        return ResponseEntity.ok(body);
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", false);
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}