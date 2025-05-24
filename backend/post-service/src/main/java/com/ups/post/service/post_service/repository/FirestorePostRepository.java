package com.ups.post.service.post_service.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.ups.post.service.post_service.model.Post;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.ups.post.service.post_service.utils.FirestoreUtils.toCompletableFuture;

@Repository
public class FirestorePostRepository {
    private static final String COLLECTION = "posts";

    private final Firestore firestore;

    public FirestorePostRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public Mono<Post> findById(String id) {
        DocumentReference docRef = firestore.collection(COLLECTION).document(id);
        ApiFuture<DocumentSnapshot> future = docRef.get();
    
        return Mono.fromFuture(toCompletableFuture(future))
                .flatMap(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Post post = documentSnapshot.toObject(Post.class);
                        return Mono.just(post);
                    } else {
                        return Mono.empty();
                    }
                });
    }

    public Mono<Post> save(Post post) {
        DocumentReference docRef = firestore.collection(COLLECTION).document(post.getId());
        ApiFuture<WriteResult> future = docRef.set(post);

        return Mono.fromFuture(toCompletableFuture(future))
            .thenReturn(post);
    }

    public Mono<Post> update(Post post) {
        DocumentReference docRef = firestore.collection(COLLECTION).document(post.getId());
    
        ApiFuture<WriteResult> future = docRef.update(
            "title", post.getTitle(),
            "content", post.getContent(),
            "createdAt", post.getCreatedAt()
        );
    
        return Mono.fromFuture(toCompletableFuture(future))
            .thenReturn(post);
    }
    

    public Mono<Void> deleteById(String id) {
        DocumentReference docRef = firestore.collection(COLLECTION).document(id);
        ApiFuture<WriteResult> future = docRef.delete();

        return Mono.fromFuture(toCompletableFuture(future)).then();
    }

    public Flux<Post> findAll() {
        CollectionReference collectionRef = firestore.collection(COLLECTION);
        ApiFuture<QuerySnapshot> future = collectionRef.get();
    
        return Mono.fromFuture(toCompletableFuture(future))
                .flatMapMany(querySnapshot -> {
                    List<QueryDocumentSnapshot> documents = querySnapshot.getDocuments();
                    return Flux.fromIterable(documents)
                            .map(doc -> doc.toObject(Post.class));
                });
    }
}
