package com.voscorp.vosmlp.applications;

import com.voscorp.vosmlp.auth.AppUser;
import com.voscorp.vosmlp.auth.AppUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Доступно только админу (правило /admin/** в SecurityConfig)
@Controller
public class AdminJournalAccessController {
    private final JournalAccessRequestService service;
    private final AppUserRepository users;

    public AdminJournalAccessController(JournalAccessRequestService service, AppUserRepository users) {
        this.service = service;
        this.users = users;
    }

    private AppUser currentUser(Authentication authentication) {
        return users.findByUsername(authentication.getName()).orElseThrow();
    }

    private String back(Long id, ApplicationService.Result r, RedirectAttributes redirect) {
        redirect.addFlashAttribute(r.ok() ? "appMessage" : "appError", r.message());
        return "redirect:/admin/applications/" + id;
    }

    @PostMapping("/admin/applications/{id}/abuse")
    public String abuse(@PathVariable Long id, @RequestParam String note,
                        Authentication authentication, RedirectAttributes redirect) {
        return back(id, service.confirmAbuse(currentUser(authentication), id, note), redirect);
    }

    @PostMapping("/admin/applications/{id}/clear")
    public String clear(@PathVariable Long id, @RequestParam String note,
                        Authentication authentication, RedirectAttributes redirect) {
        return back(id, service.clear(currentUser(authentication), id, note), redirect);
    }
}