package com.voscorp.vosmlp;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class StudentController {
    private final AttendanceRepository attendances;
    private final AbsenceApplicationRepository applications;

    public StudentController(AttendanceRepository attendances, AbsenceApplicationRepository applications) {
        this.attendances = attendances;
        this.applications = applications;
    }

    @GetMapping("/student")
    public String student(Model model) {
        model.addAttribute("absences", attendances.findByStatus(AttendanceStatus.UNEXCUSED));
        model.addAttribute("applications", applications.findAll());
        return "student";
    }

    @PostMapping("/student/apply")
    public String apply(@RequestParam Long attendanceId,
                        @RequestParam String reason,
                        @RequestParam String documentType) {
        Attendance attendance = attendances.findById(attendanceId).orElseThrow();
        applications.save(new AbsenceApplication(attendance, reason, documentType));
        return "redirect:/student";
    }
}