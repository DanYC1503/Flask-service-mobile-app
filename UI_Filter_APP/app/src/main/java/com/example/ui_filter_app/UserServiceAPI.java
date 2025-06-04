package com.example.ui_filter_app;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;

import java.util.List;

public interface UserServiceAPI {
    @POST("/users/register")
    Call<UserProfileDTO> registerUser(@Body UserProfileDTO user);

    @GET("/users/{uid}")
    Call<UserProfileDTO> getUser(@Path("uid") String uid);

    @PUT("/users/{uid}")
    Call<UserProfileDTO> updateUser(@Path("uid") String uid, @Body UserProfileDTO user);

    @DELETE("/users/{uid}")
    Call<Void> deleteUser(@Path("uid") String uid);

    @GET("/users")
    Call<List<UserProfileDTO>> getAllUsers();

    @GET("/upload/{postId}")
    Call<String> getImageUrl(@Path("postId") String postId);
}




