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

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<String> uploadImage(@RequestPart("file") FilePart filePart) {
        return imageService.uploadImage(filePart, "default");
    }

    @PostMapping(value = "/process", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<String> processImage(@RequestPart("file") FilePart filePart,
                                    @RequestPart("method") String method,
                                    @RequestPart("mask_size") Integer maskSize) {
        return imageService.processAndUploadImage(filePart, "default", method, maskSize);
    }
    @DeleteMapping
    public Mono<ResponseEntity<Object>> deleteImageByUrl(@RequestBody String imageUrlOrName) {
        return imageService.deleteImageByUrl(imageUrlOrName)
                .then(Mono.just(ResponseEntity.noContent().build()))
                .onErrorResume(e -> Mono.just(ResponseEntity.notFound().build()));
    }

}