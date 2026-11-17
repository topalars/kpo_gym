package com.example.fitlife.config;
import com.example.fitlife.service.FitLifeService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
@Configuration
public class Bootstrap {
 @Bean ApplicationRunner administrator(FitLifeService service,@Value("${fitlife.admin-email}") String email,@Value("${fitlife.admin-password}") String password){return args->service.bootstrap(email,password);}
}
