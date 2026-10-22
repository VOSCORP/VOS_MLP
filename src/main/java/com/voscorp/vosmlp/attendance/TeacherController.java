package com.voscorp.vosmlp.attendance;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TeacherController {
    private final AttendanceRepository attendances;

    public TeacherController(AttendanceRepository attendances) {
        this.attendances = attendances;
    }

    @GetMapping("/teacher")
    public String teacher(Model model) {
        model.addAttribute("items", attendances.findAll());
        return "attendance/attendance";
    }
}