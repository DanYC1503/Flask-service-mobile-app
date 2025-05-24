package com.ups.post.service.post_service.model;

import com.google.auto.value.AutoValue.Builder;
import com.google.cloud.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Like {
    private String id;
    private String postId;
    private String userId;
    private Timestamp likedAt;
}
