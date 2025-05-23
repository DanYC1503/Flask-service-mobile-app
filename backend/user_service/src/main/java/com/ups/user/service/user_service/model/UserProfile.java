package com.ups.user.service.user_service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {
    private String uid; // Id Firebase
    private String email;
    private String displayName;
    private String photoUrl;
    private String bio;
}
