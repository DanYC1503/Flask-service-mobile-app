package com.ups.auth.service.auth_service.controller;

import com.ups.auth.service.auth_service.service.AuthService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import com.ups.auth.service.auth_service.dto.Response;

@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/verify-token")
    public Mono<ResponseEntity<Response<String>>> verifyToken(@RequestHeader("Authorization") String authHeader) {
        log.info("Verificando token recibido en header.");

        String token = authHeader.replace("Bearer ", "");

        return authService.verifyToken(token)
            .map(decodedToken -> {
                log.info("Token válido para usuario: {}", decodedToken.getUid());
                Response<String> response = new Response<>(HttpStatus.OK.value(), "Token válido","User ID: " + decodedToken.getUid());
                return ResponseEntity.ok(response);
            })
            .onErrorResume(e -> {
                log.error("Error al verificar token: {}", e.getMessage());
                Response<String> response = new Response<>(HttpStatus.UNAUTHORIZED.value(), "Token inválido", null);
                return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response));
            });
    }
}