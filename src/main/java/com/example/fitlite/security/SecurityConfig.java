package com.example.fitlite.security;

import com.example.fitlite.UserRole;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(
                    auth -> auth
                            .requestMatchers("/", "/auth/**").permitAll()
                            .requestMatchers("/panel/**").authenticated()
                            .requestMatchers("/admin/panel").hasRole(UserRole.SUPERADMIN.name())
                            .anyRequest().permitAll()
                ).build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
