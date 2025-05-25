package com.ups.user.service.user_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

import jakarta.annotation.PostConstruct;

@Configuration
public class SecurityConfig {

    @PostConstruct
    public void init() {
        System.out.println("🔐 SecurityConfig loaded in User Service");
    }


    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(csrf -> csrf.disable())
            .authorizeExchange(exchange -> exchange
                .anyExchange().permitAll()
            )
            .build();
    }
}
