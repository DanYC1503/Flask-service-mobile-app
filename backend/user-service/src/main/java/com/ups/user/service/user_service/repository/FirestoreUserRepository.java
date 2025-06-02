package com.ups.user.service.user_service.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.ups.user.service.user_service.model.UserProfile;

import static com.ups.user.service.user_service.utils.FirestoreUtils.toCompletableFuture;

import java.util.List;

import org.springframework.stereotype.Repository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class FirestoreUserRepository {
    private static final String COLLECTION = "users";

    private final Firestore firestore;

    public FirestoreUserRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public Mono<UserProfile> findById(String uid) {
        DocumentReference docRef = firestore.collection(COLLECTION).document(uid);
        ApiFuture<DocumentSnapshot> future = docRef.get();
    
        return Mono.fromFuture(toCompletableFuture(future))
                .flatMap(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        UserProfile user = documentSnapshot.toObject(UserProfile.class);
                        return Mono.just(user);
                    } else {
                        return Mono.empty();
                    }
                });
    }

    public Mono<UserProfile> save(UserProfile user) {
        DocumentReference docRef = firestore.collection(COLLECTION).document(user.getUid());
        ApiFuture<WriteResult> future = docRef.set(user);

        return Mono.fromFuture(toCompletableFuture(future))
            .thenReturn(user);
    }

    public Mono<UserProfile> update(UserProfile user) {
        DocumentReference docRef = firestore.collection(COLLECTION).document(user.getUid());
    
        ApiFuture<WriteResult> future = docRef.update(
            "email", user.getEmail(),
            "userName", user.getUserName(),
            "displayName", user.getDisplayName(),
            "bio", user.getBio()
        );
    
        return Mono.fromFuture(toCompletableFuture(future))
            .thenReturn(user);
    }

    public Mono<Void> deleteById(String uid) {
        DocumentReference docRef = firestore.collection(COLLECTION).document(uid);
        ApiFuture<WriteResult> future = docRef.delete();

        return Mono.fromFuture(toCompletableFuture(future)).then();
    }

    public Flux<UserProfile> findAll() {
        CollectionReference collectionRef = firestore.collection(COLLECTION);
        ApiFuture<QuerySnapshot> future = collectionRef.get();
    
        return Mono.fromFuture(toCompletableFuture(future))
                .flatMapMany(querySnapshot -> {
                    List<QueryDocumentSnapshot> documents = querySnapshot.getDocuments();
                    return Flux.fromIterable(documents)
                            .map(doc -> doc.toObject(UserProfile.class));
                });
    }
}
