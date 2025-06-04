package com.example.ui_filter_app;

public class Post {
    private String id;
    private String title;
    private String content;
    private String authorId;
    private FireStoreTimestamp createdAt;  // Use this class to map the Firestore timestamp

    // Default constructor for Gson
    public Post() {}

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getAuthorId() {
        return authorId;
    }

    public FireStoreTimestamp getCreatedAt() {
        return createdAt;
    }
}
