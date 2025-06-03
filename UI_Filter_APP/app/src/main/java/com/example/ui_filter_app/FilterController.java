package com.example.ui_filter_app;

import android.util.Log;
import android.graphics.Bitmap;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.MediaType;


public class FilterController {
    private static final String URL_API = "http://10.0.2.2:8080/upload/process"; // Use 10.0.2.2 for localhost in Android emulator
    public static String selectedMethod = "none"; // default

    public interface FilterCallback {
        void onSuccess(String base64Image);
        void onError(String errorMessage);
    }

    public static void sendFilterRequest(Bitmap bitmap, String method, FilterCallback callback) {
        OkHttpClient client = new OkHttpClient();

        // Convert Bitmap to byte array
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] imageBytes = stream.toByteArray();

        RequestBody imageBody = RequestBody.create(imageBytes, MediaType.parse("image/jpeg"));

        MultipartBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", "upload.jpg", imageBody)
                .addFormDataPart("method", method)
                .addFormDataPart("mask_size", "13")
                .build();

        Request request = new Request.Builder()
                .url(URL_API)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Failed: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onError("Error: " + response.code());
                    return;
                }

                String body = response.body().string();
                // Extract only the base64 string (you can use JSON parsing)
                String base64 = extractBase64FromJson(body);
                callback.onSuccess(base64);
            }
        });
    }

    private static String extractBase64FromJson(String json) {
        // Very naive approach — use a real JSON parser like org.json or Moshi if needed
        int start = json.indexOf("\"output_image_base64\":\"") + 24;
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }
    public void applyFilter(String method) {
        selectedMethod = method;
        Log.d("FilterController", "Filter applied: " + method);
    }

}
