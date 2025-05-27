package com.ups.api.gateway.api_gateway.security;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import static com.ups.api.gateway.api_gateway.utils.FirebaseUtils.toCompletableFuture;

import com.google.firebase.auth.FirebaseAuth;

import reactor.core.publisher.Mono;

@Component
@Order(1)
public class AuthenticationFilter implements GlobalFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        System.out.println("🔐 [Gateway] Authorization header: " + authHeader);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("❌ [Gateway] No token or malformed token");
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        return Mono.fromFuture(toCompletableFuture(FirebaseAuth.getInstance().verifyIdTokenAsync(token)))
            .flatMap(decodedToken -> {
                System.out.println("✅ [Gateway] Token verified for UID: " + decodedToken.getUid());
                exchange.getRequest().mutate()
                    .header("X-User-Id", decodedToken.getUid())
                    .build();
                return chain.filter(exchange);
            })
            .onErrorResume(e -> {
                System.out.println("❌ [Gateway] Token verification failed: " + e.getMessage());
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            });
    }

}