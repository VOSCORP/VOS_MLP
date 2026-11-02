package com.voscorp.vosmlp.documents;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DocumentsController {

    @GetMapping("/documents")
    public String page() {
        return "documents/documents";
    }
}