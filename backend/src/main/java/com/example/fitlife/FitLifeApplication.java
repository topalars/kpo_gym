package com.example.fitlife;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class)
public class FitLifeApplication {
    public static void main(String[] args) { SpringApplication.run(FitLifeApplication.class, args); }
}
