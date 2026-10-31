package com.voscorp.vosmlp.notifications;

import com.voscorp.vosmlp.auth.AppUser;
import com.voscorp.vosmlp.auth.AppUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class NotificationController {
    private final NotificationRepository repo;
    private final AppUserRepository users;

    public NotificationController(NotificationRepository repo, AppUserRepository users) {
        this.repo = repo;
        this.users = users;
    }

    private AppUser currentUser(Authentication authentication) {
        return users.findByUsername(authentication.getName()).orElseThrow();
    }

    @GetMapping("/notifications")
    public String list(Authentication authentication, Model model) {
        model.addAttribute("items", repo.findByRecipientOrderByCreatedAtDesc(currentUser(authentication)));
        return "notifications/notifications";
    }

    @PostMapping("/notifications/read-all")
    public String readAll(Authentication authentication) {
        List<Notification> unread = repo.findByRecipientAndSeenFalse(currentUser(authentication));
        for (Notification n : unread) { n.markSeen(); }
        repo.saveAll(unread);
        return "redirect:/notifications";
    }

    // Вызывается из JavaScript при открытии страницы: отдаёт ещё не показанные toast-уведомления
    @PostMapping("/notifications/toasts")
    @ResponseBody
    public List<Map<String, String>> toasts(Authentication authentication) {
        List<Notification> fresh = repo.findByRecipientAndToastedFalse(currentUser(authentication));
        List<Map<String, String>> result = new ArrayList<>();
        for (Notification n : fresh) {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("text", n.getText());
            m.put("link", n.getLink() == null ? "" : n.getLink());
            result.add(m);
            n.markToasted();
        }
        repo.saveAll(fresh);
        return result;
    }
}