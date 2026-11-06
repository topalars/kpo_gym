package com.example.fitlite.web.controllers_for_pages;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PublicPageController {

    @GetMapping("/")
    public String getLandingPage() {
        return "landing";
    }

    @GetMapping("/error")
    public String getErrorPage() {
        return "error";
    }
}
