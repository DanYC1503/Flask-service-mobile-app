package com.ups.user.service.user_service.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord.CreateRequest;
import com.ups.user.service.user_service.dto.UserProfileDTO;
import com.ups.user.service.user_service.model.UserProfile;
import com.ups.user.service.user_service.repository.FirestoreUserRepository;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class UserService {

    private final FirestoreUserRepository repository;
    private final FirebaseAuth firebaseAuth;

    public UserService(FirestoreUserRepository repository, FirebaseAuth firebaseAuth) {
        this.repository = repository;
        this.firebaseAuth = firebaseAuth;
    }

    public Mono<UserProfile> registerUser(UserProfileDTO dto) {
        // 1. Crear en Firebase Auth
        return Mono.fromCallable(() -> {
            CreateRequest request = new CreateRequest()
                .setEmail(dto.getEmail())
                .setPassword(dto.getPassword());

            return firebaseAuth.createUser(request);
        }).flatMap(userRecord -> {
            // 2. Guardar en Firestore
            UserProfile user = new UserProfile(
                userRecord.getUid(),
                dto.getEmail(),
                dto.getUserName(),
                dto.getDisplayName(),
                dto.getBio()
            );

            return repository.save(user)
                .onErrorResume(e -> {
                    // 3. Rollback si Firestore falla
                    return Mono.fromCallable(() -> {
                        firebaseAuth.deleteUser(userRecord.getUid());
                        return null;
                    }).then(Mono.error(new RuntimeException("Error al guardar en Firestore: " + e.getMessage())));
                });

        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<UserProfile> getUserById(String uid) {
        return repository.findById(uid);
    }

    public Mono<UserProfile> updateUser(String uid, UserProfileDTO dto) {
        return repository.findById(uid)
                .flatMap(existingUser -> {
                    existingUser.setEmail(dto.getEmail());
                    existingUser.setUserName(dto.getUserName());
                    existingUser.setDisplayName(dto.getDisplayName());
                    existingUser.setBio(dto.getBio());
                    return repository.update(existingUser);
                });
    }

    public Mono<Void> deleteUser(String uid) {
        return repository.deleteById(uid);
    }

    public Flux<UserProfile> getAllUsers() {
        return repository.findAll();
    }
}
