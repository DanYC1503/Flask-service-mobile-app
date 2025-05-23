package com.ups.auth.service.auth_service.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;

@Configuration
public class SecurityConfig {

    @Bean
    public ReactiveAuthenticationManager firebaseAuthenticationManager() {
        return new FirebaseAuthenticationManager();
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http,
                                                            ReactiveAuthenticationManager authManager) {
        http.csrf(csrf -> csrf.disable());
        BearerTokenAuthenticationConverter converter = new BearerTokenAuthenticationConverter();
        AuthenticationWebFilter authWebFilter = new AuthenticationWebFilter(authManager);
        authWebFilter.setServerAuthenticationConverter(converter);

        return http
            .securityMatcher(ServerWebExchangeMatchers.anyExchange())
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/public/**").permitAll()
                .anyExchange().authenticated()
            )
            .addFilterAt(authWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .build();
    }

}