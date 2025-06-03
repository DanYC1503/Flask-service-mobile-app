package com.ups.image.service.image_service.service;

import jakarta.annotation.PostConstruct;

import com.google.api.client.util.Value;
import com.google.api.gax.paging.Page;
import com.google.cloud.storage.*;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;
import org.springframework.http.MediaType;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ImageService {

    private final Storage storage;
    private final WebClient webClient;
    private Bucket bucket;
    private final AtomicInteger counter = new AtomicInteger(0);
    

    private String[] flaskUrls;

    public ImageService(Storage storage, WebClient.Builder builder) {
        this.storage = storage;
        this.webClient = builder.build();
    }

    @PostConstruct
    public void init() {
        this.bucket = storage.get("upsglam.firebasestorage.app");
        if (bucket == null) {
            throw new RuntimeException("No se pudo obtener el bucket: " + "upsglam.firebasestorage.app");
        }
        this.flaskUrls = new String[] {
        "http://flask-filter-1:5000/process",
        "http://flask-filter-2:5000/process",
        "http://flask-filter-3:5000/process"
    };;
    }

    private String getNextFlaskUrl() {
        int index = counter.getAndUpdate(i -> (i + 1) % flaskUrls.length);
        return flaskUrls[index];
    }


    public Mono<String> processImage(FilePart filePart, String filename, String method, Integer maskSize) {
        return DataBufferUtils.join(filePart.content())
            .flatMap(dataBuffer -> {
                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                dataBuffer.read(bytes);
                DataBufferUtils.release(dataBuffer);
                return sendToFlask(bytes, filename, method, maskSize); // ✅ Now returns Mono<String>
            });
    }



    private Mono<String> sendToFlask(byte[] imageBytes, String filename, String method, Integer maskSize) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("image", imageBytes)
            .filename(filename)
            .contentType(MediaType.APPLICATION_OCTET_STREAM);
        builder.part("method", method);
        builder.part("mask_size", maskSize);

        String flaskUrl = getNextFlaskUrl();

        return webClient.post()
            .uri(flaskUrl)
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(builder.build()))
            .retrieve()
            .bodyToMono(String.class); // ✅ Expect a Base64 string
    }





    public Mono<String> uploadImage(FilePart filePart, String postId) {
        return DataBufferUtils.join(filePart.content())
            .map(dataBuffer -> {
                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                dataBuffer.read(bytes);
                DataBufferUtils.release(dataBuffer);
                return bytes;
            })
            .flatMap(bytes -> {
                String fileName = UUID.randomUUID().toString() + "-" + filePart.filename();
                String path = String.format("images/posts/%s/%s", postId, fileName);
                
                bucket.create(path, bytes, filePart.headers().getContentType().toString());
                
                URL signedUrl = storage.signUrl(
                    BlobInfo.newBuilder(bucket.getName(), path).build(),
                    1, TimeUnit.HOURS,
                    Storage.SignUrlOption.withV4Signature()
                );
                
                return Mono.just(signedUrl.toString());
            });
    }

    public Mono<String> getImageUrl(String postId) {
        return Mono.fromCallable(() -> {
            // Buscar el primer archivo en la ruta del post
            String prefix = String.format("images/posts/%s/", postId);
            Page<Blob> blobs = storage.list(bucket.getName(), Storage.BlobListOption.prefix(prefix));

            for (Blob blob : blobs.iterateAll()) {
                if (!blob.isDirectory()) {
                    URL signedUrl = storage.signUrl(
                        BlobInfo.newBuilder(bucket.getName(), blob.getName()).build(),
                        1, TimeUnit.HOURS,
                        Storage.SignUrlOption.withV4Signature()
                    );
                    return signedUrl.toString();
                }
            }

            throw new RuntimeException("No se encontró ninguna imagen para el post: " + postId);
        });
    }

    public Mono<Void> deleteImageByUrl(String postId) {
        return Mono.fromRunnable(() -> {
            String prefix = String.format("images/posts/%s/", postId);
            Page<Blob> blobs = storage.list(bucket.getName(), Storage.BlobListOption.prefix(prefix));

            for (Blob blob : blobs.iterateAll()) {
                if (!blob.isDirectory()) {
                    blob.delete();
                    return;
                }
            }

            throw new RuntimeException("No se encontró ninguna imagen para eliminar en el post: " + postId);
        });
    }

}