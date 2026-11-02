package com.voscorp.vosmlp.admin;

import com.voscorp.vosmlp.audit.AuditAction;
import com.voscorp.vosmlp.audit.AuditService;
import com.voscorp.vosmlp.auth.AppUser;
import com.voscorp.vosmlp.auth.AppUserRepository;
import com.voscorp.vosmlp.discipline.*;
import com.voscorp.vosmlp.students.Student;
import com.voscorp.vosmlp.students.StudentRepository;
import com.voscorp.vosmlp.students.StudentStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AdminController {
    private final DisciplinaryNoticeRepository notices;
    private final ExpulsionProposalRepository proposals;
    private final StudentRepository students;
    private final AuditService audit;
    private final AppUserRepository users;

    public AdminController(DisciplinaryNoticeRepository notices, ExpulsionProposalRepository proposals,
                           StudentRepository students, AuditService audit, AppUserRepository users) {
        this.notices = notices;
        this.proposals = proposals;
        this.students = students;
        this.audit = audit;
        this.users = users;
    }

    private AppUser currentUser(Authentication authentication) {
        return users.findByUsername(authentication.getName()).orElseThrow();
    }

    @GetMapping("/admin")
    public String admin(Model model) {
        model.addAttribute("notices", notices.findAll());
        model.addAttribute("proposals", proposals.findAll());
        return "admin/admin";
    }

    @PostMapping("/admin/proposals/{id}/confirm")
    public String confirmExpulsion(@PathVariable Long id, Authentication authentication) {
        AppUser actor = currentUser(authentication);
        ExpulsionProposal p = proposals.findById(id).orElseThrow();
        if (p.getStatus() == ProposalStatus.PENDING_ADMIN_APPROVAL) {
            p.setStatus(ProposalStatus.CONFIRMED);
            Student student = p.getStudent();
            student.setStatus(StudentStatus.EXPELLED);
            students.save(student);
            proposals.save(p);
            audit.log(actor, AuditAction.EXPULSION_CONFIRMED, student.getFullName(), "учится", "отчислен",
                    "Замечаний: " + p.getNoticeCount());
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/proposals/{id}/reject")
    public String rejectExpulsion(@PathVariable Long id, Authentication authentication) {
        AppUser actor = currentUser(authentication);
        ExpulsionProposal p = proposals.findById(id).orElseThrow();
        if (p.getStatus() == ProposalStatus.PENDING_ADMIN_APPROVAL) {
            p.setStatus(ProposalStatus.REJECTED);
            proposals.save(p);
            audit.log(actor, AuditAction.EXPULSION_REJECTED, p.getStudent().getFullName(),
                    "ждёт решения", "отклонено", null);
        }
        return "redirect:/admin";
    }
}