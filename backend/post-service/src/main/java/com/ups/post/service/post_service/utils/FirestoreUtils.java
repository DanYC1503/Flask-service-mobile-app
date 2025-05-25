package com.ups.post.service.post_service.utils;

import java.util.concurrent.CompletableFuture;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;

public class FirestoreUtils {
    public static <T> CompletableFuture<T> toCompletableFuture(ApiFuture<T> apiFuture) {
        CompletableFuture<T> completableFuture = new CompletableFuture<>();
        ApiFutures.addCallback(apiFuture, new ApiFutureCallback<>() {
            @Override
            public void onFailure(Throwable t) {
                completableFuture.completeExceptionally(t);
            }

            @Override
            public void onSuccess(T result) {
                completableFuture.complete(result);
            }
        }, Runnable::run); // ejecuta en el hilo actual

        return completableFuture;
    }

}
