package com.voscorp.vosmlp.journal;

import com.voscorp.vosmlp.students.Student;
import com.voscorp.vosmlp.students.StudentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
public class JournalController {
    private final StudyGroupRepository groups;
    private final SubjectRepository subjects;
    private final JournalColumnRepository columns;
    private final JournalEntryRepository entries;
    private final StudentRepository students;

    public JournalController(StudyGroupRepository groups, SubjectRepository subjects,
                             JournalColumnRepository columns, JournalEntryRepository entries,
                             StudentRepository students) {
        this.groups = groups;
        this.subjects = subjects;
        this.columns = columns;
        this.entries = entries;
        this.students = students;
    }

    @GetMapping("/journal")
    public String journal(@RequestParam(required = false) Long groupId,
                          @RequestParam(required = false) Long subjectId,
                          Model model) {
        List<StudyGroup> allGroups = groups.findAll();
        List<Subject> allSubjects = subjects.findAll();
        if (allGroups.isEmpty() || allSubjects.isEmpty()) {
            model.addAttribute("empty", true);
            return "journal/journal";
        }

        StudyGroup group = groupId == null ? allGroups.get(0) : groups.findById(groupId).orElse(allGroups.get(0));
        Subject subject = subjectId == null ? allSubjects.get(0) : subjects.findById(subjectId).orElse(allSubjects.get(0));

        List<JournalColumn> cols = columns.findByStudyGroupAndSubjectOrderByColumnDateAsc(group, subject);
        List<Student> groupStudents = students.findAll().stream()
                .filter(s -> s.getStudyGroup() != null && s.getStudyGroup().getId().equals(group.getId()))
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
            rows.add(new JournalRow(s.getId(), s.getFullName(), cells, averageOf(studentEntries)));
        }

        model.addAttribute("groups", allGroups);
        model.addAttribute("subjects", allSubjects);
        model.addAttribute("selectedGroupId", group.getId());
        model.addAttribute("selectedSubjectId", subject.getId());
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
                                                        @RequestParam(defaultValue = "false") boolean excused) {
        Student student = students.findById(studentId).orElse(null);
        JournalColumn column = columns.findById(columnId).orElse(null);
        if (student == null || column == null) {
            return error("Студент или столбец не найден");
        }

        CellStatus st;
        try {
            st = CellStatus.valueOf(status);
        } catch (IllegalArgumentException ex) {
            return error("Неизвестный статус ячейки");
        }

        Integer sc = null;
        if (score != null && !score.isBlank()) {
            try {
                sc = Integer.parseInt(score.trim());
            } catch (NumberFormatException ex) {
                return error("Оценка должна быть целым числом");
            }
            if (sc < 0 || sc > 100) {
                return error("Оценка должна быть от 0 до 100. Значение не сохранено.");
            }
        }
        if (st == CellStatus.MAKEUP && sc != null && sc > 80) {
            return error("На отработке максимум 80 баллов. Чтобы получить больше, студент выполняет дополнительное задание преподавателя.");
        }
        if (st == CellStatus.LATE && (lateMinutes == null || lateMinutes < 1)) {
            return error("Укажите, на сколько минут опоздал студент");
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
            saved = entries.save(new JournalEntry(student, column, sc, st, lateMinutes, ex));
        } else {
            existing.setScore(sc);
            existing.setStatus(st);
            existing.setLateMinutes(lateMinutes);
            existing.setExcused(ex);
            saved = entries.save(existing);
        }

        List<JournalColumn> cols = columns.findByStudyGroupAndSubjectOrderByColumnDateAsc(
                column.getStudyGroup(), column.getSubject());
        String average = averageOf(entries.findByStudentAndJournalColumnIn(student, cols));

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
        return ResponseEntity.ok(body);
    }

    private ResponseEntity<Map<String, Object>> error(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", false);
        body.put("message", message);
        return ResponseEntity.badRequest().body(body);
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