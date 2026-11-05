package com.voscorp.vosmlp.documents;

import com.voscorp.vosmlp.auth.AppUser;
import com.voscorp.vosmlp.auth.AppUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Страница доступна только админу (правило /admin/** в SecurityConfig)
@Controller
public class AdminDocumentsController {
    private final DocumentService service;
    private final AppUserRepository users;

    public AdminDocumentsController(DocumentService service, AppUserRepository users) {
        this.service = service;
        this.users = users;
    }

    private AppUser currentUser(Authentication authentication) {
        return users.findByUsername(authentication.getName()).orElseThrow();
    }

    @GetMapping("/admin/documents")
    public String page(Model model) {
        model.addAttribute("owners", service.studentAccounts());
        model.addAttribute("types", DocType.importantTypes());
        model.addAttribute("rows", service.importantRows());
        return "admin/student-documents";
    }

    @PostMapping("/admin/documents/upload")
    public String upload(@RequestParam Long ownerId, @RequestParam String type,
                         @RequestParam("file") MultipartFile file,
                         Authentication authentication, RedirectAttributes redirect) {
        String error = service.uploadImportant(currentUser(authentication), ownerId, type, file);
        if (error != null) { redirect.addFlashAttribute("docError", error); }
        else { redirect.addFlashAttribute("docMessage", "Документ прикреплён, студент получил уведомление."); }
        return "redirect:/admin/documents";
    }

    @PostMapping("/admin/documents/{id}/delete")
    public String delete(@PathVariable Long id, Authentication authentication, RedirectAttributes redirect) {
        String error = service.deleteImportant(currentUser(authentication), id);
        if (error != null) { redirect.addFlashAttribute("docError", error); }
        else { redirect.addFlashAttribute("docMessage", "Документ удалён."); }
        return "redirect:/admin/documents";
    }
}