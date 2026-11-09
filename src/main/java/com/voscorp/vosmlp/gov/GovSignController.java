package com.voscorp.vosmlp.gov;

import com.voscorp.vosmlp.auth.AppUser;
import com.voscorp.vosmlp.auth.AppUserRepository;
import com.voscorp.vosmlp.signatures.SignatureRequest;
import com.voscorp.vosmlp.signatures.SignatureService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

// Временная страница VOS Gov: «Подписать», затем «Подтвердить»
@Controller
public class GovSignController {
    private final SignatureService signatures;
    private final AppUserRepository users;

    public GovSignController(SignatureService signatures, AppUserRepository users) {
        this.signatures = signatures;
        this.users = users;
    }

    private AppUser currentUser(Authentication authentication) {
        return users.findByUsername(authentication.getName()).orElseThrow();
    }

    @GetMapping("/gov/sign/{token}")
    public String page(@PathVariable String token, Authentication authentication, Model model) {
        AppUser user = currentUser(authentication);
        SignatureRequest r = signatures.findFor(user, token);
        if (r == null) { return "redirect:/"; }
        model.addAttribute("req", r);
        model.addAttribute("signerName", user.getFullName());
        return "gov/sign";
    }

    @PostMapping("/gov/sign/{token}/sign")
    public String sign(@PathVariable String token, Authentication authentication) {
        SignatureRequest r = signatures.findFor(currentUser(authentication), token);
        if (r == null) { return "redirect:/"; }
        signatures.sign(r);
        return "redirect:/gov/sign/" + token;
    }

    @PostMapping("/gov/sign/{token}/confirm")
    public String confirm(@PathVariable String token, Authentication authentication) {
        SignatureRequest r = signatures.findFor(currentUser(authentication), token);
        if (r == null) { return "redirect:/"; }
        signatures.confirm(r);
        return "redirect:" + r.getReturnUrl(); // в College придёт toast-уведомление от обработчика
    }

    @PostMapping("/gov/sign/{token}/cancel")
    public String cancel(@PathVariable String token, Authentication authentication) {
        SignatureRequest r = signatures.findFor(currentUser(authentication), token);
        if (r == null) { return "redirect:/"; }
        signatures.cancel(r);
        return "redirect:" + r.getReturnUrl();
    }
}