package com.turnofacil.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Deshabilitar CSRF para permitir peticiones POST/PUT de Postman
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll() // Permite acceso libre a todas las rutas sin pedir login ni token
                );

        return http.build();
    }
}