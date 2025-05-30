package com.ups.api.gateway.api_gateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;

import static com.ups.api.gateway.api_gateway.utils.FirebaseUtils.toCompletableFuture;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;

import reactor.core.publisher.Mono;

@Component
@Order(1)
public class AuthenticationFilter implements GlobalFilter {

    private final FirebaseAuth firebaseAuth;
    private final WebClient userServiceClient;

    public AuthenticationFilter(@Value("${user.service.url}") String userServiceUrl) {
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.userServiceClient = WebClient.builder()
                .baseUrl(userServiceUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod() != null ? exchange.getRequest().getMethod().name() : "";


        // Permitir POST /users/register sin validar token
        if (path.equals("/users/register") && "POST".equalsIgnoreCase(method)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        System.out.println("[Gateway] Authorization header: " + authHeader);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("[Gateway] No token or malformed token");
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        return Mono.fromFuture(toCompletableFuture(firebaseAuth.verifyIdTokenAsync(token)))
            .flatMap((FirebaseToken decodedToken) -> {
                String uid = decodedToken.getUid();
                System.out.println("[Gateway] Token verified for UID: " + uid);

                return userServiceClient.get()
                        .uri("/users/{uid}", uid)
                        .exchangeToMono(response -> {
                            if (response.statusCode().is2xxSuccessful()) {
                                return Mono.just(uid); // usuario existe, continúa
                            } else {
                                return Mono.error(new RuntimeException("User not found in user-service"));
                            }
                        });
            })
            .flatMap(uid -> {
                ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(exchange.getRequest().mutate()
                        .header("X-User-Id", uid)
                        .build())
                    .build();
                return chain.filter(mutatedExchange);
            })
            .onErrorResume(e -> {
                System.out.println("[Gateway] Authentication failed: " + e.getMessage());
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            });
    }
}