package com.example.ui_filter_app;

import com.google.gson.annotations.SerializedName;

public class CommentDTO {
    private String userId;
    @SerializedName("content")
    private String text;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}