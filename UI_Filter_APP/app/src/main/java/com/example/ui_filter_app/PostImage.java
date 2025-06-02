package com.example.ui_filter_app;


public class PostImage {
    private String postId;
    private String imageUrl;

    public PostImage(String postId, String imageUrl) {
        this.postId = postId;
        this.imageUrl = imageUrl;
    }

    public String getPostId() {
        return postId;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
