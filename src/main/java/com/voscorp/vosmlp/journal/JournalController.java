package com.voscorp.vosmlp.journal;

import com.voscorp.vosmlp.students.Student;
import com.voscorp.vosmlp.students.StudentRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
            List<Integer> scores = new ArrayList<>();
            for (JournalColumn c : cols) {
                JournalEntry e = byKey.get(s.getId() + ":" + c.getId());
                cells.add(makeCell(e));
                if (e != null && e.getScore() != null && e.getStatus() != CellStatus.ABSENT) {
                    scores.add(e.getScore());
                }
            }
            String average = scores.isEmpty() ? "—"
                    : String.format("%.2f", scores.stream().mapToInt(Integer::intValue).average().orElse(0));
            rows.add(new JournalRow(s.getFullName(), cells, average));
        }

        model.addAttribute("groups", allGroups);
        model.addAttribute("subjects", allSubjects);
        model.addAttribute("selectedGroupId", group.getId());
        model.addAttribute("selectedSubjectId", subject.getId());
        model.addAttribute("columnViews", columnViews);
        model.addAttribute("rows", rows);
        return "journal/journal";
    }

    private CellView makeCell(JournalEntry e) {
        if (e == null) {
            return new CellView("", "empty", "");
        }
        switch (e.getStatus()) {
            case LATE: {
                String text = (e.getScore() != null ? e.getScore() + " · " : "") + e.getLateMinutes() + " мин";
                return new CellView(text, "late", "Опоздание на " + e.getLateMinutes() + " мин");
            }
            case ABSENT:
                return new CellView("Н", "absent",
                        e.isExcused() ? "Не пришёл, с причиной" : "Не пришёл, без причины");
            case MAKEUP:
                return new CellView(e.getScore() + " (отр.)", "makeup", "Отработка: максимум 80 баллов");
            default:
                return new CellView(e.getScore() == null ? "" : String.valueOf(e.getScore()), "normal", "");
        }
    }
}