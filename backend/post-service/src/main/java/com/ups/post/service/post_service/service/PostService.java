package com.ups.post.service.post_service.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.cloud.Timestamp;
import com.ups.post.service.post_service.dto.PostDTO;
import com.ups.post.service.post_service.model.Post;
import com.ups.post.service.post_service.repository.FirestorePostRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class PostService {

    @Autowired
    private FirestorePostRepository repository;

    public Mono<Post> createPost(PostDTO dto) {
        String id = UUID.randomUUID().toString();
        Post post = new Post(id, dto.getTitle(), dto.getContent(), dto.getAuthorId(), Timestamp.now());
        return repository.save(post);
    }

    public Mono<Post> updatePost(String id, PostDTO dto) { 
        return repository.findById(id)
            .flatMap(existingPost -> {
                existingPost.setTitle(dto.getTitle());
                existingPost.setContent(dto.getContent());
                existingPost.setCreatedAt(Timestamp.now());    
                return repository.update(existingPost);
            })
            .switchIfEmpty(Mono.error(new RuntimeException("Post not found with id: " + id)));
    }
    
    
    public Flux<Post> getAllPosts() {
        return repository.findAll();
    }
    
    public Mono<Post> getPostById(String id) {
        return repository.findById(id);
    }
    
    public Mono<Void> deletePost(String id) {
        return repository.deleteById(id);
    }
    
}