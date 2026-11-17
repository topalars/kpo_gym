package com.example.fitlife.controller;
import com.example.fitlife.service.FitLifeService;
import com.example.fitlife.dto.Requests.*;
import com.example.fitlife.entity.UserAccount;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.web.csrf.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.context.SecurityContextRepository;
import java.util.*;

@RestController @RequestMapping("/api/auth")
public class AuthController {
 private final FitLifeService service;private final SecurityContextRepository contexts;
 public AuthController(FitLifeService service,SecurityContextRepository contexts){this.service=service;this.contexts=contexts;}
 @GetMapping("/csrf") public Map<String,String> csrf(CsrfToken token){return Map.of("token",token.getToken(),"headerName",token.getHeaderName());}
 @GetMapping("/session") public Map<String,Object> session(){return service.userView(service.me());}
 @PostMapping("/login") public Map<String,Object> login(@Valid @RequestBody Login r,HttpServletRequest req,HttpServletResponse res){return establish(service.authenticate(r),req,res);}
 @PostMapping("/register") public Map<String,Object> register(@Valid @RequestBody Register r,HttpServletRequest req,HttpServletResponse res){return establish(service.register(r),req,res);}
 private Map<String,Object> establish(UserAccount user,HttpServletRequest req,HttpServletResponse res){
  if(req.getSession(false)!=null)req.changeSessionId();else req.getSession(true);
  var auth=UsernamePasswordAuthenticationToken.authenticated(user.id,null,List.of(new SimpleGrantedAuthority(user.role)));
  new CsrfAuthenticationStrategy(new HttpSessionCsrfTokenRepository()).onAuthentication(auth,req,res);
  var context=SecurityContextHolder.createEmptyContext();context.setAuthentication(auth);SecurityContextHolder.setContext(context);contexts.saveContext(context,req,res);
  return service.userView(user);
 }
}
