package com.ups.post.service.post_service.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.ups.post.service.post_service.model.Comment;
import com.ups.post.service.post_service.model.Like;
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

    public Mono<Void> addComment(String postId, Comment comment) {
        CollectionReference commentsRef = firestore.collection("posts").document(postId).collection("comments");
        ApiFuture<WriteResult> future = commentsRef.document(comment.getId()).set(comment);
        return Mono.fromFuture(() -> toCompletableFuture(future)).then();
    }

    public Mono<Comment> updateComment(Comment comment) {
        DocumentReference commentRef = firestore.collection("posts")
            .document(comment.getPostId()).collection("comments").document(comment.getId());
    
        Map<String, Object> updates = new HashMap<>();
        updates.put("content", comment.getContent());
    
        ApiFuture<WriteResult> future = commentRef.update(updates);
        return Mono.fromFuture(toCompletableFuture(future))
            .thenReturn(comment);
    }

    public Mono<Comment> getComment(String postId, String commentId) {
        DocumentReference commentRef = firestore.collection("posts")
            .document(postId).collection("comments")
            .document(commentId);
    
        ApiFuture<DocumentSnapshot> future = commentRef.get();
    
        return Mono.fromFuture(toCompletableFuture(future))
            .flatMap(docSnapshot -> {
                if (docSnapshot.exists()) {
                    Comment comment = docSnapshot.toObject(Comment.class);
                    return Mono.just(comment);
                } else {
                    return Mono.error(new RuntimeException("Comment not found with id: " + commentId));
                }
            });
    }
    

    public Mono<Void> deleteComment(String postId, String commentId) {
        DocumentReference commentRef = firestore.collection("posts")
            .document(postId).collection("comments").document(commentId);
    
        ApiFuture<WriteResult> future = commentRef.delete();
        return Mono.fromFuture(toCompletableFuture(future)).then();
    }    
    
    public Flux<Comment> getComments(String postId) {
        CollectionReference commentsRef = firestore.collection("posts").document(postId).collection("comments");
        ApiFuture<QuerySnapshot> future = commentsRef.get();
    
        return Mono.fromFuture(() -> toCompletableFuture(future))
                    .flatMapMany(snapshot -> Flux.fromIterable(snapshot.getDocuments())
                        .map(doc -> doc.toObject(Comment.class)));
    }
    
    public Mono<Void> addLike(String postId, String userId) {
        DocumentReference likeRef = firestore.collection("posts").document(postId)
                                            .collection("likes").document(userId);
        Like like = new Like(userId, postId, userId, Timestamp.now());
        ApiFuture<WriteResult> future = likeRef.set(like);
        return Mono.fromFuture(() -> toCompletableFuture(future)).then();
    }
    
    public Mono<Void> removeLike(String postId, String userId) {
        DocumentReference likeRef = firestore.collection("posts").document(postId)
                                            .collection("likes").document(userId);
        ApiFuture<WriteResult> future = likeRef.delete();
        return Mono.fromFuture(() -> toCompletableFuture(future)).then();
    }
    
    public Mono<Long> countLikes(String postId) {
        CollectionReference likesRef = firestore.collection("posts").document(postId)
                                                .collection("likes");
        ApiFuture<QuerySnapshot> future = likesRef.get();
        return Mono.fromFuture(() -> toCompletableFuture(future))
                    .map(snapshot -> (long) snapshot.size());
    }
    
}
