package com.example.ui_filter_app;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface PostServiceAPI {
    @POST("posts/post")
    Call<Post> uploadPostToFeed(@Body PostDTO postDTO);

    @GET("posts")
    Call<List<Post>> getAllPosts();

    @POST("posts/{postId}/likes/{userId}")
    Call<Void> addLike(@Path("postId") String postId, @Path("userId") String userId);

    @DELETE("posts/{postId}/likes/{userId}")
    Call<Void> removeLike(@Path("postId") String postId, @Path("userId") String userId);

    @GET("posts/{postId}/likes/count")
    Call<Long> getLikeCount(@Path("postId") String postId);

    @GET("posts/{postId}/comments")
    Call<List<Comment>> getComments(@Path("postId") String postId);

    @POST("posts/{postId}/comments")
    Call<Comment> addComment(@Path("postId") String postId, @Body CommentDTO commentDTO);

    @DELETE("posts/{postId}/comments/{commentId}")
    Call<Void> deleteComment(@Path("postId") String postId, @Path("commentId") String commentId);
}
