package com.example.ui_filter_app;

public class PostDTO {
    private String title;
    private String content;
    private String authorId;

    public PostDTO(String title, String content, String authorId) {
        this.title = title;
        this.content = content;
        this.authorId = authorId;
    }

    // Getters (Retrofit/Gson requires them)
    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getAuthorId() {
        return authorId;
    }
}
