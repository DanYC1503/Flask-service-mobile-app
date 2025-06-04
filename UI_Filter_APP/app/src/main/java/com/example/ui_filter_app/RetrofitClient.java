package com.example.ui_filter_app;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class RetrofitClient {
    private static final String BASE_URL = "http://192.168.186.44:8080/";
    private static Retrofit retrofit = null;

    public static Retrofit getClient(Context context) {
        if (retrofit == null) {

            // Create logging interceptor and set desired log level
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY); // Logs request and response body

            OkHttpClient.Builder httpClient = new OkHttpClient.Builder();

            // Add logging interceptor
            httpClient.addInterceptor(loggingInterceptor);

            // Interceptor to add Firebase token header if available
            httpClient.addInterceptor(chain -> {
                Request original = chain.request();
                String token = context.getSharedPreferences("app_session", MODE_PRIVATE)
                        .getString("firebase_token", null);

                if (token != null) {
                    Request newRequest = original.newBuilder()
                            .header("Authorization", "Bearer " + token)
                            .build();
                    return chain.proceed(newRequest);
                }
                return chain.proceed(original);
            });

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(httpClient.build())
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
