package com.ups.post.service.post_service.model;

import com.google.auto.value.AutoValue.Builder;
import com.google.cloud.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {
    private String id;
    private String title;
    private String content;
    private String authorId;
    private Timestamp createdAt;
}