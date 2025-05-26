package com.example.ui_filter_app;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

import com.example.ui_filter_app.UserProfileDTO;

public interface UserServiceAPI {
    @POST("/users")
    Call<UserProfileDTO> registerUser(@Body UserProfileDTO user);
}
