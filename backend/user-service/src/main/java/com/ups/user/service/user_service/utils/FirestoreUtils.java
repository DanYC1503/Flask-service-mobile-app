package com.ups.user.service.user_service.utils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.google.api.core.ApiFuture;

public class FirestoreUtils {
    public static <T> CompletableFuture<T> toCompletableFuture(ApiFuture<T> apiFuture) {
        CompletableFuture<T> completable = new CompletableFuture<>();
        apiFuture.addListener(() -> {
            try {
                completable.complete(apiFuture.get());
            } catch (InterruptedException | ExecutionException e) {
                completable.completeExceptionally(e);
            }
        }, Runnable::run);
        return completable;
    }
}
