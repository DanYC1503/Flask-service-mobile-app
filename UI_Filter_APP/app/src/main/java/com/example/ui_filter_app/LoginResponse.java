package com.example.ui_filter_app;

public class LoginResponse {
    private String uid;
    private String username;
    private String token; // opcional, si tu backend lo da

    // Getters y setters

    public String getUid() {
        return uid;
    }

    public String getUsername() {
        return username;
    }

    public String getToken() {
        return token;
    }
}
