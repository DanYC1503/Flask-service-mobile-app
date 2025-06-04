package com.ups.post.service.post_service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

import com.ups.post.service.post_service.dto.CommentDTO;
import com.ups.post.service.post_service.dto.PostDTO;
import com.ups.post.service.post_service.model.Comment;
import com.ups.post.service.post_service.model.Post;
import com.ups.post.service.post_service.service.PostService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.web.bind.annotation.PutMapping;


@Slf4j
@RestController
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping("/post")
    public Mono<ResponseEntity<Post>> createPost(@RequestBody PostDTO dto) {
        return postService.createPost(dto)
                .map(p -> ResponseEntity.status(HttpStatus.CREATED).body(p));
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<Post>> updatePost(@PathVariable String id, @RequestBody PostDTO dto) {
        return postService.updatePost(id, dto)
                .map(p -> ResponseEntity.status(HttpStatus.OK).body(p));
    }

    @GetMapping
    public Flux<Post> getAllPosts() {
        return postService.getAllPosts();
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Post>> getPostById(@PathVariable String id) {
        return postService.getPostById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deletePost(@PathVariable String id) {
        return postService.deletePost(id)
                .thenReturn(ResponseEntity.noContent().build());
    }

    @PostMapping("/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Void> addComment(@PathVariable String postId, @RequestBody CommentDTO dto) {
        return postService.addComment(postId, dto);
    }

    @PutMapping("/{postId}/comments/{commentId}")
    public Mono<Comment> updateComment(
        @PathVariable String postId,
        @PathVariable String commentId,
        @RequestBody CommentDTO dto
    ) {
        return postService.updateComment(postId, commentId, dto);
    }

    @DeleteMapping("/{postId}/comments/{commentId}")
    public Mono<ResponseEntity<Void>>  deleteComment(
        @PathVariable String postId,
        @PathVariable String commentId
    ) {
        return postService.deleteComment(postId, commentId)
            .thenReturn(ResponseEntity.noContent().build());
    }

    @GetMapping("/{postId}/comments")
    public Flux<Comment> getComments(@PathVariable String postId) {
        return postService.getComments(postId);
    }

    @PostMapping("/{postId}/likes/{userId}")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Void> addLike(@PathVariable String postId, @PathVariable String userId) {
        return postService.addLike(postId, userId);
    }

    @DeleteMapping("/{postId}/likes/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> removeLike(@PathVariable String postId, @PathVariable String userId) {
        return postService.removeLike(postId, userId);
    }

    @GetMapping("/{postId}/likes/count")
    public Mono<Long> countLikes(@PathVariable String postId) {
        return postService.countLikes(postId);
    }
}
