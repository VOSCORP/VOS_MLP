package com.voscorp.vosmlp.documents;

import com.voscorp.vosmlp.auth.AppUser;
import com.voscorp.vosmlp.auth.AppUserRepository;
import com.voscorp.vosmlp.auth.UserRole;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
public class DocumentsController {
    private final DocumentService service;
    private final AppUserRepository users;
    private final com.voscorp.vosmlp.applications.ApplicationService applications;

    public DocumentsController(DocumentService service, AppUserRepository users,
                               com.voscorp.vosmlp.applications.ApplicationService applications) {
        this.service = service;
        this.users = users;
        this.applications = applications;
    }

    private AppUser currentUser(Authentication authentication) {
        return users.findByUsername(authentication.getName()).orElseThrow();
    }

    @GetMapping("/documents")
    public String page(@RequestParam(required = false) String tab, Authentication authentication, Model model) {
        AppUser user = currentUser(authentication);
        boolean studentSide = user.getRole() == UserRole.STUDENT || user.getRole() == UserRole.STAROSTA;

        String active = tab;
        if (!"important".equals(active) && !"mine".equals(active) && !"requests".equals(active)) {
            active = studentSide ? "important" : "mine";
        }
        if ("important".equals(active) && !studentSide) { active = "mine"; }

        model.addAttribute("tab", active);
        model.addAttribute("showImportant", studentSide);
        model.addAttribute("importantSlots", studentSide ? service.importantSlots(user) : List.of());
        model.addAttribute("govSlots", service.govSlots(user));
        model.addAttribute("otherDocs", service.otherDocs(user));
        model.addAttribute("requestTypes", applications.typesFor(user));
        model.addAttribute("myApplications", applications.mine(user));
        return "documents/documents";
    }

    @PostMapping("/documents/upload")
    public String upload(@RequestParam String title, @RequestParam String purpose,
                         @RequestParam("file") MultipartFile file,
                         Authentication authentication, RedirectAttributes redirect) {
        String error = service.uploadOther(currentUser(authentication), title, purpose, file);
        if (error != null) { redirect.addFlashAttribute("docError", error); }
        else { redirect.addFlashAttribute("docMessage", "Документ загружен."); }
        return "redirect:/documents?tab=mine";
    }

    @PostMapping("/documents/{id}/sync")
    public String sync(@PathVariable Long id, Authentication authentication, RedirectAttributes redirect) {
        redirect.addFlashAttribute("docMessage", service.syncExisting(currentUser(authentication), id));
        return "redirect:/documents?tab=mine";
    }

    @PostMapping("/documents/sync-type")
    public String syncType(@RequestParam String type, Authentication authentication, RedirectAttributes redirect) {
        String message;
        try {
            message = service.syncFromGov(currentUser(authentication), DocType.valueOf(type));
        } catch (IllegalArgumentException e) {
            message = "Неизвестный тип документа";
        }
        redirect.addFlashAttribute("docMessage", message);
        return "redirect:/documents?tab=mine";
    }

    @PostMapping("/documents/{id}/delete")
    public String delete(@PathVariable Long id, Authentication authentication, RedirectAttributes redirect) {
        String error = service.deleteOther(currentUser(authentication), id);
        if (error != null) { redirect.addFlashAttribute("docError", error); }
        else { redirect.addFlashAttribute("docMessage", "Документ удалён."); }
        return "redirect:/documents?tab=mine";
    }

    @GetMapping("/documents/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id, Authentication authentication) {
        UserDocument d = service.forDownload(currentUser(authentication), id);
        if (d == null) { return ResponseEntity.notFound().build(); }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(d.getFileName(), StandardCharsets.UTF_8).build().toString())
                .body(d.getContent());
    }
}