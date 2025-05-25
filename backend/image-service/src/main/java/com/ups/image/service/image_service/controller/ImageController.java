package com.ups.image.service.image_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;

import com.ups.image.service.image_service.service.ImageService;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/upload")
public class ImageController {

    @Autowired
    private ImageService imageService;

    @PostMapping(value = "/{postId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<String> uploadImage(@PathVariable String postId,
                                    @RequestPart("file") FilePart filePart) {
        return imageService.uploadImage(filePart, postId);
    }

    @GetMapping("/{postId}")
    public Mono<ResponseEntity<String>> getImageUrl(@PathVariable String postId) {
        return imageService.getImageUrl(postId)
                .map(url -> ResponseEntity.ok().body(url))
                .onErrorResume(e -> Mono.just(ResponseEntity.notFound().build()));
    }

    @DeleteMapping("/{postId}")
    public Mono<ResponseEntity<Object>> deleteImage(@PathVariable String postId) {
        return imageService.deleteImage(postId)
                .then(Mono.just(ResponseEntity.noContent().build()))
                .onErrorResume(e -> Mono.just(ResponseEntity.notFound().build()));
    }
}