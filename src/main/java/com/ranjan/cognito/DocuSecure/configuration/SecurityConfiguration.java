package com.ranjan.cognito.DocuSecure.configuration;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebSecurity

public class SecurityConfiguration {
     @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        

        http.csrf(c->c.disable())
            // Enable OAuth2 Login for Thymeleaf frontend
            .oauth2Login(Customizer.withDefaults())
            .cors(cors->cors.configurationSource(corsConfigurationSource()))
            // Enable OAuth2 Resource Server with JWT validation for the backend API
            .oauth2ResourceServer((oauth2) -> oauth2.jwt(Customizer.withDefaults()))
            // Configure access rules
            .authorizeHttpRequests((c)->{
                c.requestMatchers("/",
                                    "/logged-out", 
                                    "/error",
                                    "/swagger-ui/**",
                                    "/v3/api-docs/**",
                                    "/swagger-ui.html").permitAll();
                c.requestMatchers(
                                            "/upload",
                                            "/download/**",
                                            "/api/**",
                                            "/custom-logout"
                                            ).authenticated();
            })
            .logout(c->c.disable());
                
                
                
        return http.build();
    }

   @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
