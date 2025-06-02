package com.example.ui_filter_app;

public class PostInfo {
    private String postId;
    private String userName;

    public PostInfo(String postId, String userName) {
        this.postId = postId;
        this.userName = userName;
    }

    public String getPostId() {
        return postId;
    }

    public String getUserName() {
        return userName;
    }
}
