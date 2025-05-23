package com.ups.user.service.user_service.service;

import com.google.cloud.firestore.Firestore;
import com.ups.user.service.user_service.dto.UserProfileDTO;
import com.ups.user.service.user_service.model.UserProfile;
import com.ups.user.service.user_service.repository.FirestoreUserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class UserService {

    @Autowired
    private FirestoreUserRepository repository;

    public Mono<UserProfile> getUserById(String uid) {
        return repository.findById(uid);
    }

    public Mono<UserProfile> createUser(String uid, UserProfileDTO dto) {
        UserProfile user = new UserProfile(uid, dto.getEmail(), dto.getDisplayName(), dto.getPhotoUrl(), dto.getBio());
        return repository.save(user);
    }

    public Mono<UserProfile> updateUser(String uid, UserProfileDTO dto) {
        return repository.findById(uid)
                .flatMap(existingUser -> {
                    existingUser.setEmail(dto.getEmail());
                    existingUser.setDisplayName(dto.getDisplayName());
                    existingUser.setPhotoUrl(dto.getPhotoUrl());
                    existingUser.setBio(dto.getBio());
                    return repository.save(existingUser);
                });
    }

    public Mono<Void> deleteUser(String uid) {
        return repository.deleteById(uid);
    }


    public Flux<UserProfile> getAllUsers() {
        return repository.findAll();
    }
}
