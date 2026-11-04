package com.voscorp.vosmlp.attendance;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

// Страница доступна только админу (правило /admin/** в SecurityConfig)
@Controller
public class AttendanceSummaryController {
    private final AttendanceSummaryService service;

    public AttendanceSummaryController(AttendanceSummaryService service) {
        this.service = service;
    }

    @GetMapping("/admin/attendance")
    public String page(Model model) {
        model.addAttribute("summary", service.build());
        return "attendance/summary";
    }
}