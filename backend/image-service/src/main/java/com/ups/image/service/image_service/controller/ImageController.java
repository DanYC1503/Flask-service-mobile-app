package com.ups.image.service.image_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
        }); // ← returns raw base64 as plain text
}


    
@PostMapping(value = "/process-and-upload/{postId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public Mono<String> processAndUploadImage(
    @PathVariable String postId,
    @RequestPart("image") FilePart image,
    @RequestPart("method") Mono<String> methodMono,
    @RequestPart("mask_size") Mono<String> maskSizeMono) {

    ObjectMapper objectMapper = new ObjectMapper();

    return Mono.zip(methodMono, maskSizeMono)
        .flatMap(tuple -> {
            String method = tuple.getT1();
            String maskSizeStr = tuple.getT2();

            int maskSize;
            try {
                maskSize = Integer.parseInt(maskSizeStr);
            } catch (NumberFormatException e) {
                return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "mask_size must be an integer"));
            }

            return imageService.processImage(image, image.filename(), method, maskSize)
                .flatMap(json -> {
                    try {
                        Map<String, Object> result = objectMapper.readValue(json, Map.class);
                        String base64 = (String) result.get("output_image_base64");
                        byte[] imageBytes = java.util.Base64.getDecoder().decode(base64);
                        return imageService.uploadProcessedImage(imageBytes, postId, image.filename());
                    } catch (Exception e) {
                        return Mono.error(new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR, "Invalid JSON response from processImage"));
                    }
                });
        });
}

    @DeleteMapping
    public Mono<ResponseEntity<Object>> deleteImageByUrl(@RequestBody String imageUrlOrName) {
        return imageService.deleteImageByUrl(imageUrlOrName)
                .then(Mono.just(ResponseEntity.noContent().build()))
                .onErrorResume(e -> Mono.just(ResponseEntity.notFound().build()));
    }

}