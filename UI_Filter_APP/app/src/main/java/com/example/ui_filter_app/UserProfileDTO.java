package com.example.ui_filter_app;

public class UserProfileDTO {

    private String uid;
    private String email;
    private String userName;
    private String displayName;
    private String bio;

    // ✅ Constructor requerido
    public UserProfileDTO(String uid,String email, String userName, String displayName, String bio) {
        this.uid = uid;
        this.email = email;
        this.userName = userName;
        this.displayName = displayName;
        this.bio = bio;
    }

    // ✅ Getters y setters si los necesitas para Retrofit/Gson

    public String id() {
        return uid;
    }
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }
}