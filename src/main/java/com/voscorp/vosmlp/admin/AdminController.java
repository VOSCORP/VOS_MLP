package com.voscorp.vosmlp.admin;

import com.voscorp.vosmlp.attendance.Attendance;
import com.voscorp.vosmlp.attendance.AttendanceRepository;
import com.voscorp.vosmlp.attendance.AttendanceStatus;
import com.voscorp.vosmlp.discipline.*;
import com.voscorp.vosmlp.documents.*;
import com.voscorp.vosmlp.students.Student;
import com.voscorp.vosmlp.students.StudentRepository;
import com.voscorp.vosmlp.students.StudentStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AdminController {
    private final AbsenceApplicationRepository applications;
    private final AttendanceRepository attendances;
    private final StudentRepository students;
    private final DisciplinaryNoticeRepository notices;
    private final ExpulsionProposalRepository proposals;
    private final MedicalApiMock medicalApi;

    public AdminController(AbsenceApplicationRepository applications,
                           AttendanceRepository attendances,
                           StudentRepository students,
                           DisciplinaryNoticeRepository notices,
                           ExpulsionProposalRepository proposals,
                           MedicalApiMock medicalApi) {
        this.applications = applications;
        this.attendances = attendances;
        this.students = students;
        this.notices = notices;
        this.proposals = proposals;
        this.medicalApi = medicalApi;
    }

    @GetMapping("/admin")
    public String admin(Model model) {
        model.addAttribute("applications", applications.findAll());
        model.addAttribute("proposals", proposals.findAll());
        model.addAttribute("notices", notices.findAll());
        return "admin/admin";
    }

    @PostMapping("/admin/{id}/verify")
    public String verify(@PathVariable Long id) {
        AbsenceApplication app = applications.findById(id).orElseThrow();
        if (app.getMedicalResult() != null) {
            return "redirect:/admin"; // уже проверено, повторно не создаём замечание
        }
        MedicalResult result = medicalApi.verify(app);
        app.setMedicalResult(result);
        applications.save(app);

        if (result != MedicalResult.VALID && !notices.existsByApplication(app)) {
            Student student = app.getAttendance().getStudent();
            notices.save(new DisciplinaryNotice(student, app, "Документ не прошёл проверку: " + result.name()));
            long count = notices.countByStudent(student);
            if (count > 3 && !proposals.existsByStudentAndStatus(student, ProposalStatus.PENDING_ADMIN_APPROVAL)) {
                proposals.save(new ExpulsionProposal(student, (int) count));
            }
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/{id}/approve")
    public String approve(@PathVariable Long id) {
        AbsenceApplication app = applications.findById(id).orElseThrow();
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

    @PostMapping("/admin/proposals/{id}/confirm")
    public String confirmExpulsion(@PathVariable Long id) {
        ExpulsionProposal p = proposals.findById(id).orElseThrow();
        if (p.getStatus() == ProposalStatus.PENDING_ADMIN_APPROVAL) {
            p.setStatus(ProposalStatus.CONFIRMED);
            Student student = p.getStudent();
            student.setStatus(StudentStatus.EXPELLED);
            students.save(student);
            proposals.save(p);
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/proposals/{id}/reject")
    public String rejectExpulsion(@PathVariable Long id) {
        ExpulsionProposal p = proposals.findById(id).orElseThrow();
        if (p.getStatus() == ProposalStatus.PENDING_ADMIN_APPROVAL) {
            p.setStatus(ProposalStatus.REJECTED);
            proposals.save(p);
        }
        return "redirect:/admin";
    }
}