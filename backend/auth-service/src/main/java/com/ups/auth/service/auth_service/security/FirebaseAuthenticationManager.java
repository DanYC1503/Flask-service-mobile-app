package com.ups.auth.service.auth_service.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import reactor.core.publisher.Mono;

public class FirebaseAuthenticationManager implements ReactiveAuthenticationManager {

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String token = authentication.getCredentials().toString();

        return Mono.fromCallable(() -> FirebaseAuth.getInstance().verifyIdToken(token))
                .map(this::buildAuthentication)
                .onErrorResume(e -> Mono.empty());
    }

    private Authentication buildAuthentication(FirebaseToken firebaseToken) {
        AbstractAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                firebaseToken.getUid(),
                firebaseToken,
                null
        );
        return auth;
    }
}