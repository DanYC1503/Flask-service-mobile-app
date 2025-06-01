package com.example.ui_filter_app;

public class UserProfileDTO {

    private String uid;
    private String email;
    private String userName;
    private String displayName;
    private String bio;
    private String password;

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getUid() {
        return uid;
    }
// ✅ Constructor requerido

    public UserProfileDTO(String uid,String email, String userName, String displayName, String bio, String password) {
        this.uid = uid;
        this.email = email;
        this.userName = userName;
        this.displayName = displayName;
        this.bio = bio;
        this.password = password;
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

    public String getPassword() { return password; } // ESTE ES EL QUE FALTA
    public void setPassword(String password) { this.password = password; }
}