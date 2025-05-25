package com.ups.user.service.user_service.controller;

import com.ups.user.service.user_service.dto.UserProfileDTO;
import com.ups.user.service.user_service.model.UserProfile;
import com.ups.user.service.user_service.service.UserService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {
    @Autowired
    private UserService userService;

    @GetMapping
    public Flux<UserProfile> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{uid}")
    public Mono<ResponseEntity<UserProfile>> getUser(@PathVariable String uid) {
        return userService.getUserById(uid)
                .map(user -> ResponseEntity.ok(user))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Mono<ResponseEntity<UserProfile>> createUser(@RequestBody UserProfileDTO dto) {
        System.out.println("📥 [User-Service] Register endpoint hit with user: " + dto.getEmail());
        return userService.createUser(dto)
                .map(user -> ResponseEntity.status(HttpStatus.CREATED).body(user));
    }

    @PutMapping("/{uid}")
    public Mono<ResponseEntity<UserProfile>> updateUser(@PathVariable String uid, @RequestBody UserProfileDTO dto) {
        return userService.updateUser(uid, dto)
                .map(user -> ResponseEntity.ok(user))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{uid}")
    public Mono<ResponseEntity<Void>> deleteUser(@PathVariable String uid) {
        return userService.deleteUser(uid)
                .thenReturn(ResponseEntity.noContent().<Void>build());
    }
}
