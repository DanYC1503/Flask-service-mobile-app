package com.ups.image.service.image_service.service;

import jakarta.annotation.PostConstruct;

import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.api.gax.paging.Page;
import com.google.cloud.storage.*;
import com.google.cloud.storage.BlobInfo;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

import java.net.URL;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class ImageService {

    private final Storage storage;
    private Bucket bucket;

    public ImageService(Storage storage) {
        this.storage = storage;
    }

    @PostConstruct
    public void init() {
        this.bucket = storage.get("upsglam.firebasestorage.app");
        if (bucket == null) {
            throw new RuntimeException("No se pudo obtener el bucket: " + "upsglam.firebasestorage.app");
        }
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

    public Mono<Void> deleteImage(String postId) {
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