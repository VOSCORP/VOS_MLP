package com.voscorp.vosmlp.applications;

import com.voscorp.vosmlp.auth.AppUser;
import com.voscorp.vosmlp.auth.AppUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Доступно только админу (правило /admin/** в SecurityConfig)
@Controller
public class AdminApplicationController {
    private final ApplicationService service;
    private final AppUserRepository users;

    public AdminApplicationController(ApplicationService service, AppUserRepository users) {
        this.service = service;
        this.users = users;
    }

    private AppUser currentUser(Authentication authentication) {
        return users.findByUsername(authentication.getName()).orElseThrow();
    }

    @GetMapping("/admin/applications")
    public String inbox(Authentication authentication, Model model) {
        model.addAttribute("items", service.inbox(currentUser(authentication)));
        return "admin/applications";
    }

    @GetMapping("/admin/applications/{id}")
    public String detail(@PathVariable Long id, Authentication authentication, Model model) {
        AppUser admin = currentUser(authentication);
        Application a = service.find(admin, id);
        if (a == null) { return "redirect:/admin/applications"; }
        model.addAttribute("v", service.view(a, admin, true));
        model.addAttribute("adminMode", true);
        return "applications/detail";
    }

    private String back(Long id, ApplicationService.Result r, RedirectAttributes redirect) {
        redirect.addFlashAttribute(r.ok() ? "appMessage" : "appError", r.message());
        return "redirect:/admin/applications/" + id;
    }

    @PostMapping("/admin/applications/{id}/verify")
    public String verify(@PathVariable Long id, Authentication authentication, RedirectAttributes redirect) {
        return back(id, service.verify(currentUser(authentication), id), redirect);
    }

    @PostMapping("/admin/applications/{id}/approve")
    public String approve(@PathVariable Long id, Authentication authentication, RedirectAttributes redirect) {
        return back(id, service.approve(currentUser(authentication), id), redirect);
    }

    @PostMapping("/admin/applications/{id}/reject")
    public String reject(@PathVariable Long id, @RequestParam(required = false) String note,
                         Authentication authentication, RedirectAttributes redirect) {
        return back(id, service.reject(currentUser(authentication), id, note), redirect);
    }
}