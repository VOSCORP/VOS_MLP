package com.voscorp.vosmlp.home;

import com.voscorp.vosmlp.auth.AppUser;
import com.voscorp.vosmlp.auth.AppUserRepository;
import com.voscorp.vosmlp.auth.UserRole;
import com.voscorp.vosmlp.messages.LetterService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    private final LetterService letters;
    private final AppUserRepository users;

    public HomeController(LetterService letters, AppUserRepository users) {
        this.letters = letters;
        this.users = users;
    }

    @GetMapping("/")
    public String home(Authentication authentication, Model model) {
        AppUser user = users.findByUsername(authentication.getName()).orElseThrow();
        if (user.getRole() == UserRole.TEACHER) {
            model.addAttribute("importantLetters", letters.importantFor(user));
        }
        return "home/home";
    }
}