package com.example.fitlite.web.controllers_for_pages;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    @GetMapping("/auth/login")
    public String getLoginPage() {
        return "auth/login";
    }

    @GetMapping("/auth/register")
    public String getRegisterPage() {
        return "auth/register";
    }

}
