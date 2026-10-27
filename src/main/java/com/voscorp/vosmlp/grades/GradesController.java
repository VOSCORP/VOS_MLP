package com.voscorp.vosmlp.grades;

import com.voscorp.vosmlp.students.Student;
import com.voscorp.vosmlp.students.StudentRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Controller
public class GradesController {
    private final GradeRepository grades;
    private final StudentRepository students;

    public GradesController(GradeRepository grades, StudentRepository students) {
        this.grades = grades;
        this.students = students;
    }

    @GetMapping("/grades")
    public String page(Model model) {
        model.addAttribute("students", students.findAll());
        model.addAttribute("grades", grades.findAll());
        model.addAttribute("summary", buildSummary());
        return "grades/grades";
    }

    @PostMapping("/grades/add")
    public String add(@RequestParam Long studentId,
                      @RequestParam String subject,
                      @RequestParam String gradeType,
                      @RequestParam int score,
                      RedirectAttributes redirect) {
        // проверка на сервере: оценка только 0–100
        if (score < 0 || score > 100) {
            redirect.addFlashAttribute("error", "Оценка должна быть от 0 до 100. Значение не сохранено.");
            return "redirect:/grades";
        }
        Student student = students.findById(studentId).orElseThrow();
        grades.save(new Grade(student, subject, gradeType, score, LocalDate.now()));
        return "redirect:/grades";
    }

    private List<StudentSummary> buildSummary() {
        List<StudentSummary> result = new ArrayList<>();
        for (Student s : students.findAll()) {
            List<Grade> list = grades.findByStudent(s);
            if (list.isEmpty()) {
                result.add(new StudentSummary(s.getFullName(), s.getGroupName(), "Оценок пока нет", "NO_DATA"));
                continue;
            }
            Map<String, Double> bySubject = list.stream().collect(
                    Collectors.groupingBy(Grade::getSubject, TreeMap::new, Collectors.averagingInt(Grade::getScore)));
            String text = bySubject.entrySet().stream()
                    .map(e -> e.getKey() + ": " + String.format("%.2f", e.getValue()))
                    .collect(Collectors.joining("; "));
            boolean ok = bySubject.values().stream().allMatch(v -> v >= 90);
            result.add(new StudentSummary(s.getFullName(), s.getGroupName(), text, ok ? "YES" : "NO"));
        }
        return result;
    }
}