package com.ups.image.service.image_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Base64;
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

    @PostMapping(value = "/process", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<String> processImage(
        @RequestPart("image") FilePart image,
        @RequestPart("method") Mono<String> methodMono,
        @RequestPart("mask_size") Mono<String> maskSizeMono) {

        return Mono.zip(methodMono, maskSizeMono)
            .flatMap(tuple -> {
                String method = tuple.getT1();
                String maskSize = tuple.getT2();

                int maskSizeInt;
                try {
                    maskSizeInt = Integer.parseInt(maskSize);
                } catch (NumberFormatException e) {
                    return Mono.error(new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "mask_size must be an integer"));
                }

                return imageService.processImage(image, image.filename(), method, maskSizeInt);
            })
            .map(base64 -> "{\"outputImageBase64\":\"" + base64 + "\"}");
    }


    @DeleteMapping
    public Mono<ResponseEntity<Object>> deleteImageByUrl(@RequestBody String imageUrlOrName) {
        return imageService.deleteImageByUrl(imageUrlOrName)
                .then(Mono.just(ResponseEntity.noContent().build()))
                .onErrorResume(e -> Mono.just(ResponseEntity.notFound().build()));
    }

}