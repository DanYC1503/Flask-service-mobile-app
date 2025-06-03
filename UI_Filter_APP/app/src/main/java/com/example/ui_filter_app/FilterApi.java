// File: FilterApi.java
package com.example.ui_filter_app;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface FilterApi {

    @Multipart
    @POST("upload/process")
    Call<ResponseBody> processImage(
        @Part MultipartBody.Part image,
        @Part("method") RequestBody method,
        @Part("mask_size") RequestBody maskSize
    );

}
