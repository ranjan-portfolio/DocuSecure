package com.ranjan.cognito.DocuSecure.configuration;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
     @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        

        http.csrf(c->c.disable())
            // Enable OAuth2 Login for Thymeleaf frontend
            .oauth2Login(Customizer.withDefaults())
            // Enable OAuth2 Resource Server with JWT validation for the backend API
            .oauth2ResourceServer((oauth2) -> oauth2.jwt(Customizer.withDefaults()))
            // Configure access rules
            .authorizeHttpRequests((c)->{
                c.requestMatchers("/","/logged-out", "/error","/swagger-ui.html").permitAll();
                c.requestMatchers(
                                            "/upload",
                                            "/download/**",
                                            "/api/**",
                                            "/custom-logout",
                                            "/v3/api-docs/**",
                                            "/swagger-ui/**").authenticated();
            })
            .logout(c->c.disable());
                
                
                
        return http.build();
    }
}
