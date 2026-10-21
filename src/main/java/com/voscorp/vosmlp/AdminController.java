package com.voscorp.vosmlp;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AdminController {
    private final AbsenceApplicationRepository applications;
    private final AttendanceRepository attendances;
    private final MedicalApiMock medicalApi;

    public AdminController(AbsenceApplicationRepository applications,
                           AttendanceRepository attendances,
                           MedicalApiMock medicalApi) {
        this.applications = applications;
        this.attendances = attendances;
        this.medicalApi = medicalApi;
    }

    @GetMapping("/admin")
    public String admin(Model model) {
        model.addAttribute("applications", applications.findAll());
        return "admin";
    }

    @PostMapping("/admin/{id}/verify")
    public String verify(@PathVariable Long id) {
        AbsenceApplication app = applications.findById(id).orElseThrow();
        app.setMedicalResult(medicalApi.verify(app));
        applications.save(app);
        return "redirect:/admin";
    }

    @PostMapping("/admin/{id}/approve")
    public String approve(@PathVariable Long id) {
        AbsenceApplication app = applications.findById(id).orElseThrow();
        // одобрить можно только после результата VALID
        if (app.getMedicalResult() == MedicalResult.VALID && app.getStatus() == ApplicationStatus.PENDING) {
            app.setStatus(ApplicationStatus.APPROVED);
            Attendance attendance = app.getAttendance();
            attendance.setStatus(AttendanceStatus.EXCUSED);
            attendances.save(attendance);
            applications.save(app);
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/{id}/reject")
    public String reject(@PathVariable Long id) {
        AbsenceApplication app = applications.findById(id).orElseThrow();
        if (app.getStatus() == ApplicationStatus.PENDING) {
            app.setStatus(ApplicationStatus.REJECTED);
            applications.save(app);
        }
        return "redirect:/admin";
    }
}