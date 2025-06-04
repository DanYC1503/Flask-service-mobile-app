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
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;

import com.ups.image.service.image_service.service.ImageService;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/upload")
public class ImageController {

    @Autowired
    private ImageService imageService;

    @PostMapping(value = "/uploadToBucket", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<String> uploadImageToBucket(@RequestPart("image") FilePart filePart) {
        return imageService.uploadImage(filePart);
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


    @PostMapping(value = "/upload/{postId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<String> uploadProcessedImage(
        @PathVariable String postId,
        @RequestPart("image") FilePart imageFile) {

        return DataBufferUtils.join(imageFile.content())
            .map(dataBuffer -> {
                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                dataBuffer.read(bytes);
                DataBufferUtils.release(dataBuffer); // Liberar el buffer manualmente
                return bytes;
            })
            .flatMap(imageBytes -> imageService.uploadProcessedImage(imageBytes, postId, imageFile.filename()));
    }
    @DeleteMapping
    public Mono<ResponseEntity<Object>> deleteImageByUrl(@RequestBody String imageUrlOrName) {
        return imageService.deleteImageByUrl(imageUrlOrName)
                .then(Mono.just(ResponseEntity.noContent().build()))
                .onErrorResume(e -> Mono.just(ResponseEntity.notFound().build()));
    }

}