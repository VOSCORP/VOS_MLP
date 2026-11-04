package com.voscorp.vosmlp.messages;

import com.voscorp.vosmlp.auth.AppUser;
import com.voscorp.vosmlp.auth.AppUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class MessagesController {
    private final LetterService service;
    private final AppUserRepository users;

    public MessagesController(LetterService service, AppUserRepository users) {
        this.service = service;
        this.users = users;
    }

    private AppUser currentUser(Authentication authentication) {
        return users.findByUsername(authentication.getName()).orElseThrow();
    }

    @GetMapping("/messages")
    public String list(Authentication authentication, Model model) {
        AppUser user = currentUser(authentication);
        model.addAttribute("threads", service.listFor(user));
        model.addAttribute("canCompose", service.canCompose(user));
        return "messages/messages";
    }

    @GetMapping("/messages/new")
    public String compose(@RequestParam(required = false) Long to,
                          @RequestParam(required = false) String subject,
                          @RequestParam(required = false) String body,
                          Authentication authentication, Model model) {
        AppUser user = currentUser(authentication);
        if (!service.canCompose(user)) { return "redirect:/messages"; }
        model.addAttribute("recipients", service.recipientsFor(user));
        model.addAttribute("selectedTo", to);
        model.addAttribute("subjectPrefill", subject == null ? "" : subject);
        model.addAttribute("bodyPrefill", body == null ? "" : body);
        return "messages/compose";
    }

    @PostMapping("/messages/send")
    public String send(@RequestParam Long recipientId, @RequestParam String subject, @RequestParam String body,
                       Authentication authentication, RedirectAttributes redirect) {
        String error = service.send(currentUser(authentication), recipientId, subject, body);
        if (error != null) {
            redirect.addFlashAttribute("error", error);
            return "redirect:/messages/new?to=" + recipientId;
        }
        redirect.addFlashAttribute("ok", "Письмо отправлено.");
        return "redirect:/messages";
    }

    @GetMapping("/messages/{id}")
    public String thread(@PathVariable Long id, Authentication authentication, Model model) {
        AppUser user = currentUser(authentication);
        LetterThread t = service.threadFor(user, id);
        if (t == null) { return "redirect:/messages"; }
        service.markRead(t, user);
        model.addAttribute("view", service.viewOf(t, user));
        model.addAttribute("letters", service.lettersOf(t, user));
        model.addAttribute("threadId", t.getId());
        model.addAttribute("canReply", t.getInitiator() != null);
        return "messages/thread";
    }

    @PostMapping("/messages/{id}/reply")
    public String reply(@PathVariable Long id, @RequestParam String body,
                        Authentication authentication, RedirectAttributes redirect) {
        String error = service.reply(currentUser(authentication), id, body);
        if (error != null) { redirect.addFlashAttribute("error", error); }
        return "redirect:/messages/" + id;
    }
}