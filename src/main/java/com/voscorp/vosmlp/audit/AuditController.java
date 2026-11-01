package com.voscorp.vosmlp.audit;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Locale;

@Controller
public class AuditController {
    private final AuditEventRepository repo;

    public AuditController(AuditEventRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/admin/audit")
    public String page(@RequestParam(required = false) String action,
                       @RequestParam(required = false) String outcome,
                       @RequestParam(required = false) String q,
                       Model model) {
        String query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        List<AuditEvent> events = repo.findTop500ByOrderByCreatedAtDesc().stream()
                .filter(e -> action == null || action.isBlank() || e.getAction().name().equals(action))
                .filter(e -> outcome == null || outcome.isBlank() || e.getOutcome().equals(outcome))
                .filter(e -> query.isEmpty()
                        || contains(e.getActorName(), query)
                        || contains(e.getTarget(), query)
                        || contains(e.getNote(), query))
                .toList();

        model.addAttribute("events", events);
        model.addAttribute("actions", AuditAction.values());
        model.addAttribute("selectedAction", action == null ? "" : action);
        model.addAttribute("selectedOutcome", outcome == null ? "" : outcome);
        model.addAttribute("q", q == null ? "" : q);
        return "audit/audit";
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }
}