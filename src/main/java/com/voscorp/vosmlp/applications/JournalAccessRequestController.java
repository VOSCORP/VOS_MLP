package com.voscorp.vosmlp.applications;

import com.voscorp.vosmlp.auth.AppUser;
import com.voscorp.vosmlp.auth.AppUserRepository;
import com.voscorp.vosmlp.auth.UserRole;
import com.voscorp.vosmlp.signatures.SignatureService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class JournalAccessRequestController {
    private final JournalAccessRequestService service;
    private final SignatureService signatures;
    private final AppUserRepository users;

    public JournalAccessRequestController(JournalAccessRequestService service, SignatureService signatures,
                                          AppUserRepository users) {
        this.service = service;
        this.signatures = signatures;
        this.users = users;
    }

    private AppUser currentUser(Authentication authentication) {
        return users.findByUsername(authentication.getName()).orElseThrow();
    }

    @GetMapping("/applications/journal-access/new")
    public String form(Authentication authentication, Model model) {
        AppUser user = currentUser(authentication);
        if (user.getRole() != UserRole.STAROSTA) { return "redirect:/documents?tab=requests"; }
        model.addAllAttributes(service.formData(user));
        return "applications/journal-access-form";
    }

    @PostMapping("/applications/journal-access")
    public String create(@RequestParam Long teacherId,
                         @RequestParam String scope,
                         @RequestParam(required = false) Long targetGroupId,
                         @RequestParam(required = false) List<Long> subjectIds,
                         @RequestParam String accessType,
                         @RequestParam String comment,
                         Authentication authentication, RedirectAttributes redirect) {
        AppUser user = currentUser(authentication);
        ApplicationService.CreateResult r = service.create(user, teacherId, scope, targetGroupId,
                subjectIds, accessType, comment);
        if (r.error() != null) {
            redirect.addFlashAttribute("appError", r.error());
            return "redirect:/applications/journal-access/new";
        }
        Application a = r.application();
        // подпись одной строкой, дальше как в заявке на посещаемость
        return signatures.require(user, a.getType().getSignAction(), a.getId(), a.getTitle(), "/documents?tab=requests");
    }

    private String back(Long id, ApplicationService.Result r, RedirectAttributes redirect) {
        redirect.addFlashAttribute(r.ok() ? "appMessage" : "appError", r.message());
        return "redirect:/applications/" + id;
    }

    @PostMapping("/applications/{id}/teacher/approve")
    public String approve(@PathVariable Long id, @RequestParam String expiresAt,
                          Authentication authentication, RedirectAttributes redirect) {
        return back(id, service.approve(currentUser(authentication), id, expiresAt), redirect);
    }

    @PostMapping("/applications/{id}/teacher/reject")
    public String reject(@PathVariable Long id, @RequestParam(required = false) String note,
                         Authentication authentication, RedirectAttributes redirect) {
        return back(id, service.reject(currentUser(authentication), id, note), redirect);
    }

    @PostMapping("/applications/{id}/teacher/forward")
    public String forward(@PathVariable Long id, @RequestParam String note,
                          Authentication authentication, RedirectAttributes redirect) {
        return back(id, service.forward(currentUser(authentication), id, note), redirect);
    }
}