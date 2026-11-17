package com.example.fitlife.config;

import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.*;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

@Configuration
public class SecurityConfig {
 @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
 @Bean SecurityContextRepository contextRepository(){return new HttpSessionSecurityContextRepository();}
 @Bean SecurityFilterChain security(HttpSecurity http,SecurityContextRepository contexts) throws Exception {
  http.authorizeHttpRequests(a->a.requestMatchers("/api/auth/**","/actuator/health","/error").permitAll().anyRequest().authenticated())
   .securityContext(s->s.securityContextRepository(contexts))
   .formLogin(AbstractHttpConfigurer::disable).httpBasic(AbstractHttpConfigurer::disable)
   .logout(l->l.logoutUrl("/api/auth/logout").logoutSuccessHandler((req,res,auth)->res.setStatus(204)).invalidateHttpSession(true).deleteCookies("JSESSIONID"))
   .exceptionHandling(e->e.authenticationEntryPoint((req,res,ex)->json(res,401,"Сессия истекла. Войдите в аккаунт."))
    .accessDeniedHandler((req,res,ex)->json(res,403,"Нет доступа или недействительный CSRF-токен. Обновите страницу.")));
  // Default session-based CSRF protection remains enabled, including login and registration.
  return http.build();
 }
 private void json(jakarta.servlet.http.HttpServletResponse res,int status,String message) throws java.io.IOException {
  res.setStatus(status);res.setContentType("application/json;charset=UTF-8");res.getWriter().write("{\"status\":"+status+",\"message\":\""+message+"\"}");
 }
}
