package com.user.login.login.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration //¿Qué es? Señala una clase que crea y define objetos que otros usarán (beans).
@EnableWebSecurity
public class WebSecurityConfig{
    
    @Bean //¿Qué es? Marca un método que crea un objeto que Spring guarda y comparte.
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
         http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/register", "/api/login").permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
