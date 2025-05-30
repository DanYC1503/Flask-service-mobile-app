package com.ups.user.service.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO {
    private String email;
    private String password;
    private String userName;
    private String displayName;
    private String bio;
}
