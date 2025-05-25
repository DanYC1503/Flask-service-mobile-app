package com.ups.user.service.user_service.service;

import com.ups.user.service.user_service.dto.UserProfileDTO;
import com.ups.user.service.user_service.model.UserProfile;
import com.ups.user.service.user_service.repository.FirestoreUserRepository;

import java.util.UUID;

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

    public Mono<UserProfile> createUser(UserProfileDTO dto) {
        String uid = UUID.randomUUID().toString();
        UserProfile user = new UserProfile(uid, dto.getEmail(), dto.getUserName(), dto.getDisplayName(), dto.getBio());
        return repository.save(user);
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
