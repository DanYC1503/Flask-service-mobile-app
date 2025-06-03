package com.example.ui_filter_app;

import android.content.Context;
import android.util.Log;
import android.graphics.Bitmap;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import okhttp3.ResponseBody;

public class FilterController {
    private static FilterApi getFilterApi(Context context) {
        return RetrofitClient.getClient(context).create(FilterApi.class);
    }

    public static String selectedMethod = "none"; // default

    public interface FilterCallback {
        void onSuccess(String base64Image);
        void onError(String errorMessage);
    }

    public static void sendFilterRequest(Context context, Bitmap bitmap, String method, FilterCallback callback) {
        FilterApi api = getFilterApi(context);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] imageBytes = stream.toByteArray();

        RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), imageBytes);
        MultipartBody.Part imagePart = MultipartBody.Part.createFormData("image", "upload.jpg", requestFile);

        RequestBody methodPart = RequestBody.create(MediaType.parse("text/plain"), method);
        RequestBody maskSizePart = RequestBody.create(MediaType.parse("text/plain"), "13");

        Call<ResponseBody> call = api.processImage(imagePart, methodPart, maskSizePart);

        call.enqueue(new Callback<ResponseBody>() {
        @Override
        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
            if (response.isSuccessful() && response.body() != null) {
                try {
                    String json = response.body().string();
                    callback.onSuccess(json); // 👈 Aquí envías el JSON crudo, no lo extraigas aún
                } catch (IOException e) {
                    callback.onError("Error reading response");
                }
            } else {
                callback.onError("Error: " + response.code());
            }
        }

        @Override
        public void onFailure(Call<ResponseBody> call, Throwable t) {
            callback.onError("Failed: " + t.getMessage());
        }
    });
    }


    public static void applyFilter(String method) {
        selectedMethod = method;
        Log.d("FilterController", "Filter applied: " + method);
    }
}
