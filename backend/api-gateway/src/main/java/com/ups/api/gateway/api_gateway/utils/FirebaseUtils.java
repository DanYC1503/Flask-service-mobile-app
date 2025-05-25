package com.ups.api.gateway.api_gateway.utils;

import com.google.api.core.ApiFuture;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class FirebaseUtils {
    public static <T> CompletableFuture<T> toCompletableFuture(ApiFuture<T> apiFuture) {
        CompletableFuture<T> completable = new CompletableFuture<>();
        Runnable listener = new Runnable() {
            @Override
            public void run() {
                try {
                    completable.complete(apiFuture.get());
                } catch (InterruptedException | ExecutionException e) {
                    completable.completeExceptionally(e);
                }
            }
        };
        apiFuture.addListener(listener, Runnable::run);
        return completable;
    }
}


