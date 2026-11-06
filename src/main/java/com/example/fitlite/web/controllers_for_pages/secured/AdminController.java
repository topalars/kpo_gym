package com.example.fitlite.web.controllers_for_pages.secured;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {

    @GetMapping("/admin/panel")
    public String getAdminPanelPage() {
        return "admin/panel";
    }
}
