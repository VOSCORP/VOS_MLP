package com.voscorp.vosmlp.journal;

import com.voscorp.vosmlp.auth.AppUser;
import com.voscorp.vosmlp.auth.AppUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class GrantController {
    private final GrantService grantService;
    private final AppUserRepository users;

    public GrantController(GrantService grantService, AppUserRepository users) {
        this.grantService = grantService;
        this.users = users;
    }

    private AppUser currentUser(Authentication authentication) {
        return users.findByUsername(authentication.getName()).orElseThrow();
    }

    @PostMapping("/journal/help/grant")
    public String grant(@RequestParam Long starostaId,
                        @RequestParam String scope,
                        @RequestParam(required = false) Long targetGroupId,
                        @RequestParam(defaultValue = "false") boolean notifyStarosta,
                        @RequestParam(defaultValue = "false") boolean notifyCurator,
                        @RequestParam String accessType,
                        @RequestParam(required = false) List<Long> subjectIds,
                        @RequestParam String expiresAt,
                        Authentication authentication,
                        RedirectAttributes redirect) {
        String error = grantService.create(currentUser(authentication), starostaId, scope, targetGroupId,
                notifyStarosta, notifyCurator, accessType, subjectIds, expiresAt);
        if (error != null) {
            redirect.addFlashAttribute("helpError", error);
        } else {
            redirect.addFlashAttribute("helpMessage", "Доступ выдан, староста получил уведомление.");
        }
        return "redirect:/journal";
    }

    @PostMapping("/journal/help/grants/{id}/close")
    public String close(@PathVariable Long id, Authentication authentication, RedirectAttributes redirect) {
        String error = grantService.close(currentUser(authentication), id);
        if (error != null) {
            redirect.addFlashAttribute("helpError", error);
        } else {
            redirect.addFlashAttribute("helpMessage", "Доступ закрыт.");
        }
        return "redirect:/journal";
    }

    @PostMapping("/journal/help/grants/{id}/done")
    public String done(@PathVariable Long id, Authentication authentication, RedirectAttributes redirect) {
        String error = grantService.markDone(currentUser(authentication), id);
        if (error != null) {
            redirect.addFlashAttribute("helpError", error);
        } else {
            redirect.addFlashAttribute("helpMessage", "Отмечено: работа выполнена. Преподаватель получил уведомление.");
        }
        return "redirect:/journal";
    }
}