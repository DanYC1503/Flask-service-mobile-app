package com.ups.auth.service.auth_service.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class AuthService {

    public Mono<FirebaseToken> verifyToken(String idToken) {
        return Mono.fromCallable(() -> FirebaseAuth.getInstance().verifyIdTokenAsync(idToken).get())
            .subscribeOn(Schedulers.boundedElastic());

    }
}
