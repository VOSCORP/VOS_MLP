package com.voscorp.vosmlp.journal;

import com.voscorp.vosmlp.auth.AppUser;
import com.voscorp.vosmlp.auth.AppUserRepository;
import com.voscorp.vosmlp.auth.UserRole;
import com.voscorp.vosmlp.students.Student;
import com.voscorp.vosmlp.students.StudentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
public class JournalController {
    private final SubjectRepository subjects;
    private final JournalColumnRepository columns;
    private final JournalEntryRepository entries;
    private final StudentRepository students;
    private final JournalCalc calc;
    private final AppUserRepository users;
    private final JournalAccess access;
    private final DashboardService dashboards;
    private final JournalNotifier notifier;

    public JournalController(SubjectRepository subjects, JournalColumnRepository columns,
                             JournalEntryRepository entries, StudentRepository students,
                             JournalCalc calc, AppUserRepository users, JournalAccess access,
                             DashboardService dashboards, JournalNotifier notifier) {
        this.subjects = subjects;
        this.columns = columns;
        this.entries = entries;
        this.students = students;
        this.calc = calc;
        this.users = users;
        this.access = access;
        this.dashboards = dashboards;
        this.notifier = notifier;
    }

    private AppUser currentUser(Authentication authentication) {
        return users.findByUsername(authentication.getName()).orElseThrow();
    }

    @GetMapping("/journal")
    public String journal(@RequestParam(required = false) Long groupId,
                          @RequestParam(required = false) Long subjectId,
                          Authentication authentication,
                          Model model) {
        AppUser user = currentUser(authentication);

        List<StudyGroup> allGroups = access.visibleGroups(user);
        if (allGroups.isEmpty()) {
            model.addAttribute("empty", true);
            return "journal/journal";
        }
        StudyGroup group = allGroups.stream()
                .filter(g -> g.getId().equals(groupId)).findFirst().orElse(allGroups.get(0));

        List<Subject> allSubjects = access.visibleSubjects(user, group);
        if (allSubjects.isEmpty()) {
            model.addAttribute("empty", true);
            return "journal/journal";
        }
        Subject subject = allSubjects.stream()
                .filter(s -> s.getId().equals(subjectId)).findFirst().orElse(allSubjects.get(0));

        List<JournalColumn> cols = columns.findByStudyGroupAndSubjectOrderByColumnDateAsc(group, subject);
        List<Student> groupStudents = students.findAll().stream()
                .filter(s -> s.getStudyGroup() != null && s.getStudyGroup().getId().equals(group.getId()))
                .filter(s -> user.getRole() != UserRole.PARENT
                        || (user.getLinkedStudent() != null && user.getLinkedStudent().getId().equals(s.getId())))
                .sorted(Comparator.comparing(Student::getFullName))
                .toList();

        Map<String, JournalEntry> byKey = new HashMap<>();
        if (!cols.isEmpty()) {
            for (JournalEntry e : entries.findByJournalColumnIn(cols)) {
                byKey.put(e.getStudent().getId() + ":" + e.getJournalColumn().getId(), e);
            }
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM");
        List<ColumnView> columnViews = new ArrayList<>();
        for (JournalColumn c : cols) {
            String label = switch (c.getType()) {
                case LESSON -> c.getColumnDate().format(fmt);
                case RK1 -> "РК1";
                case RK2 -> "РК2";
                case CREDIT_WEEK -> "Зачётная неделя";
                case ATTESTATION -> "Аттестация";
            };
            String css = c.getType() == ColumnType.LESSON ? "" : "special";
            columnViews.add(new ColumnView(label, c.getTask() == null ? "" : c.getTask(), css));
        }

        List<JournalRow> rows = new ArrayList<>();
        for (Student s : groupStudents) {
            List<CellView> cells = new ArrayList<>();
            List<JournalEntry> studentEntries = new ArrayList<>();
            for (JournalColumn c : cols) {
                JournalEntry e = byKey.get(s.getId() + ":" + c.getId());
                cells.add(new CellView(s.getId(), c.getId(), e));
                if (e != null) { studentEntries.add(e); }
            }
            ScholarshipResult sr = calc.scholarship(s);
            rows.add(new JournalRow(s.getId(), s.getFullName(), cells, averageOf(studentEntries),
                    calc.finalScore(studentEntries, cols.size()), "YES".equals(sr.getStatus())));
        }

        boolean viewerIsStudentSide = user.getRole() == UserRole.STUDENT
                || user.getRole() == UserRole.STAROSTA || user.getRole() == UserRole.PARENT;
        if (viewerIsStudentSide && user.getLinkedStudent() != null) {
            model.addAttribute("dashboard", dashboards.build(user.getLinkedStudent()));
        }
        model.addAttribute("groups", allGroups);
        model.addAttribute("subjects", allSubjects);
        model.addAttribute("selectedGroupId", group.getId());
        model.addAttribute("selectedSubjectId", subject.getId());
        model.addAttribute("editable", access.canEdit(user, group, subject));
        model.addAttribute("columnViews", columnViews);
        model.addAttribute("rows", rows);
        return "journal/journal";
    }

    // Сохранение одной ячейки (вызывается из JavaScript)
    @PostMapping("/journal/cell")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveCell(@RequestParam Long studentId,
                                                        @RequestParam Long columnId,
                                                        @RequestParam String status,
                                                        @RequestParam(required = false) String score,
                                                        @RequestParam(required = false) Integer lateMinutes,
                                                        @RequestParam(defaultValue = "false") boolean excused,
                                                        Authentication authentication) {
        AppUser user = currentUser(authentication);
        Student student = students.findById(studentId).orElse(null);
        JournalColumn column = columns.findById(columnId).orElse(null);
        if (student == null || column == null) {
            return error(HttpStatus.BAD_REQUEST, "Студент или столбец не найден");
        }
        // проверка прав на сервере
        if (!access.canEdit(user, column.getStudyGroup(), column.getSubject())) {
            return error(HttpStatus.FORBIDDEN, "У вас нет прав на правку этого журнала");
        }

        CellStatus st;
        try {
            st = CellStatus.valueOf(status);
        } catch (IllegalArgumentException ex) {
            return error(HttpStatus.BAD_REQUEST, "Неизвестный статус ячейки");
        }

        Integer sc = null;
        if (score != null && !score.isBlank()) {
            try {
                sc = Integer.parseInt(score.trim());
            } catch (NumberFormatException ex) {
                return error(HttpStatus.BAD_REQUEST, "Оценка должна быть целым числом");
            }
            if (sc < 0 || sc > 100) {
                return error(HttpStatus.BAD_REQUEST, "Оценка должна быть от 0 до 100. Значение не сохранено.");
            }
        }
        if (st == CellStatus.MAKEUP && sc != null && sc > 80) {
            return error(HttpStatus.BAD_REQUEST,
                    "На отработке максимум 80 баллов. Чтобы получить больше, студент выполняет дополнительное задание преподавателя.");
        }
        if (st == CellStatus.LATE && (lateMinutes == null || lateMinutes < 1)) {
            return error(HttpStatus.BAD_REQUEST, "Укажите, на сколько минут опоздал студент");
        }
        if (st == CellStatus.ABSENT) { sc = null; }
        if (st != CellStatus.LATE) { lateMinutes = null; }
        boolean ex = st == CellStatus.ABSENT && excused;

        JournalEntry existing = entries.findByStudentAndJournalColumn(student, column).orElse(null);
        JournalEntry saved = null;
        if (st == CellStatus.NORMAL && sc == null) {
            // пустая ячейка: запись удаляем, ноль не ставим
            if (existing != null) { entries.delete(existing); }
        } else if (existing == null) {
            JournalEntry created = new JournalEntry(student, column, sc, st, lateMinutes, ex);
            created.markGradedBy(user.getFullName());
            saved = entries.save(created);
        } else {
            existing.setScore(sc);
            existing.setStatus(st);
            existing.setLateMinutes(lateMinutes);
            existing.setExcused(ex);
            existing.markGradedBy(user.getFullName());
            saved = entries.save(existing);
        }

        if (saved != null) {
            notifier.notifyCell(student, column.getSubject().getName(), saved, user.getFullName());
        }
        List<JournalColumn> cols = columns.findByStudyGroupAndSubjectOrderByColumnDateAsc(
                column.getStudyGroup(), column.getSubject());
        List<JournalEntry> studentEntries = entries.findByStudentAndJournalColumnIn(student, cols);
        String average = averageOf(studentEntries);
        String finalScore = calc.finalScore(studentEntries, cols.size());

        CellView cell = new CellView(studentId, columnId, saved);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", true);
        body.put("text", cell.getText());
        body.put("css", cell.getCss());
        body.put("title", cell.getTitle());
        body.put("status", cell.getStatus());
        body.put("score", cell.getScore());
        body.put("lateMinutes", cell.getLateMinutes());
        body.put("excused", cell.isExcused());
        body.put("average", average);
        body.put("finalScore", finalScore);
        return ResponseEntity.ok(body);
    }

    // Данные для панели «достоин повышенной стипендии».
    // Статус виден всем (это синяя галочка), подробности по предметам только тем, кому можно.
    @GetMapping("/journal/student/{id}/scholarship")
    @ResponseBody
    public Map<String, Object> scholarship(@PathVariable Long id, Authentication authentication) {
        AppUser user = currentUser(authentication);
        Student student = students.findById(id).orElseThrow();
        ScholarshipResult result = calc.scholarship(student);
        boolean details = access.canSeeScholarshipDetails(user, student);

        List<Map<String, Object>> subjectList = new ArrayList<>();
        if (details) {
            for (ScholarshipResult.Line line : result.getLines()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("subject", line.getSubject());
                m.put("average", line.getAverage());
                m.put("count", line.getCount());
                m.put("need", line.getNeed());
                m.put("ok", line.isOk());
                subjectList.add(m);
            }
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", student.getFullName());
        body.put("status", result.getStatus());
        body.put("restricted", !details);
        body.put("subjects", subjectList);
        return body;
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", false);
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }

    // Средний по баллам; пропуски и пустые ячейки в расчёт не входят
    private String averageOf(Collection<JournalEntry> list) {
        List<Integer> scores = new ArrayList<>();
        for (JournalEntry e : list) {
            if (e.getScore() != null && e.getStatus() != CellStatus.ABSENT) {
                scores.add(e.getScore());
            }
        }
        if (scores.isEmpty()) { return "—"; }
        return String.format("%.2f", scores.stream().mapToInt(Integer::intValue).average().orElse(0));
    }
}