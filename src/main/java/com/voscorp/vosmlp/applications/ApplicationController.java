package com.voscorp.vosmlp.applications;

import com.voscorp.vosmlp.auth.AppUser;
import com.voscorp.vosmlp.auth.AppUserRepository;
import com.voscorp.vosmlp.signatures.SignatureService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class ApplicationController {
    private final ApplicationService service;
    private final SignatureService signatures;
    private final AppUserRepository users;

    public ApplicationController(ApplicationService service, SignatureService signatures, AppUserRepository users) {
        this.service = service;
        this.signatures = signatures;
        this.users = users;
    }

    private AppUser currentUser(Authentication authentication) {
        return users.findByUsername(authentication.getName()).orElseThrow();
    }

    @GetMapping("/applications/attendance/new")
    public String attendanceForm(Authentication authentication, Model model) {
        AppUser user = currentUser(authentication);
        if (!ApplicationType.ATTENDANCE_CHANGE.getRoles().contains(user.getRole())) {
            return "redirect:/documents?tab=requests";
        }
        model.addAttribute("absences", service.absencesFor(user));
        return "applications/attendance-form";
    }

    @PostMapping("/applications/attendance")
    public String attendanceCreate(@RequestParam(required = false) List<Long> recordIds,
                                   @RequestParam String certificateNumber,
                                   @RequestParam String comment,
                                   @RequestParam("file") MultipartFile file,
                                   Authentication authentication, RedirectAttributes redirect) {
        AppUser user = currentUser(authentication);
        ApplicationService.CreateResult r = service.createAttendanceChange(user, recordIds, certificateNumber, comment, file);
        if (r.error() != null) {
            redirect.addFlashAttribute("appError", r.error());
            return "redirect:/applications/attendance/new";
        }
        Application a = r.application();
        // подпись одной строкой: пользователь уходит в VOS Gov, потом возвращается на «Заявки»
        return signatures.require(user, a.getType().getSignAction(), a.getId(), a.getTitle(), "/documents?tab=requests");
    }

    @PostMapping("/applications/{id}/sign")
    public String sign(@PathVariable Long id, Authentication authentication, RedirectAttributes redirect) {
        AppUser user = currentUser(authentication);
        Application a = service.find(user, id);
        if (a == null || !a.getApplicant().getId().equals(user.getId())
                || a.getStatus() != ApplicationStatus.PENDING_SIGNATURE || a.getType().getSignAction() == null) {
            redirect.addFlashAttribute("docError", "Эту заявку подписать нельзя");
            return "redirect:/documents?tab=requests";
        }
        return signatures.require(user, a.getType().getSignAction(), a.getId(), a.getTitle(), "/documents?tab=requests");
    }

    @GetMapping("/applications/{id}")
    public String detail(@PathVariable Long id, Authentication authentication, Model model) {
        AppUser user = currentUser(authentication);
        Application a = service.find(user, id);
        if (a == null) { return "redirect:/documents?tab=requests"; }
        model.addAttribute("v", service.view(a, user, false));
        model.addAttribute("adminMode", false);
        return "applications/detail";
    }
}