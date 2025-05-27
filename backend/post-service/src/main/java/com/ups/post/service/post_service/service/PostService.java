package com.ups.post.service.post_service.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.cloud.Timestamp;
import com.ups.post.service.post_service.dto.CommentDTO;
import com.ups.post.service.post_service.dto.PostDTO;
import com.ups.post.service.post_service.model.Comment;
import com.ups.post.service.post_service.model.Post;
import com.ups.post.service.post_service.repository.FirestorePostRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class PostService {

    @Autowired
    private FirestorePostRepository repository;

    public Mono<Post> createPost(PostDTO dto) {
        String postId = UUID.randomUUID().toString();
        Post post = new Post(postId, dto.getTitle(), dto.getContent(), dto.getAuthorId(), Timestamp.now());
        return repository.save(post);
    }

    public Mono<Post> updatePost(String postId, PostDTO dto) { 
        return repository.findById(postId)
            .flatMap(existingPost -> {
                existingPost.setTitle(dto.getTitle());
                existingPost.setContent(dto.getContent());
                existingPost.setCreatedAt(Timestamp.now());    
                return repository.update(existingPost);
            })
            .switchIfEmpty(Mono.error(new RuntimeException("Post not found with id: " + postId)));
    }
    
    public Flux<Post> getAllPosts() {
        return repository.findAll();
    }
    
    public Mono<Post> getPostById(String postId) {
        return repository.findById(postId);
    }
    
    public Mono<Void> deletePost(String postId) {
        return repository.deleteById(postId);
    }

    public Mono<Void> addComment(String postId, CommentDTO dto) {
        String commentId = UUID.randomUUID().toString();
        Comment comment = new Comment(commentId, postId, dto.getUserId(), dto.getContent(), Timestamp.now());
        return repository.addComment(postId, comment);
    }

    public Mono<Comment> updateComment(String postId, String commentId, CommentDTO dto) {
        return repository.getComment(postId, commentId)
            .flatMap(existingComment -> {
                existingComment.setContent(dto.getContent());
                existingComment.setCreatedAt(Timestamp.now());
                return repository.updateComment(existingComment);
            })
            .switchIfEmpty(Mono.error(new RuntimeException("Comment not found with id: " + commentId)));
    }

    public Mono<Void> deleteComment(String postId, String commentId){
        return repository.deleteComment(postId, commentId);
    }

    public Flux<Comment> getComments(String postId){
        return repository.getComments(postId);
    }

    public Mono<Void> addLike(String postId, String userId){
        return repository.addLike(postId, userId);
    }

    public Mono<Void> removeLike(String postId, String userId){
        return repository.removeLike(postId, userId);
    }

    public Mono<Long> countLikes(String postId){
        return repository.countLikes(postId);
    }
    
}