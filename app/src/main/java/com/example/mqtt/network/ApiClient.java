package com.example.mqtt.network;

import android.util.Log;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String TAG = "ApiClient"; // Tag để ghi log
    // URL cơ sở cho API (lưu ý: cần cập nhật nếu ngrok URL hết hạn)
    private static final String BASE_URL = "https://b511-42-114-85-136.ngrok-free.app/";
    private static Retrofit retrofit = null; // Instance Retrofit

    // Lấy instance Retrofit
    public static Retrofit getClient() {
        if (retrofit == null) {
            Log.d(TAG, "Khởi tạo Retrofit với URL: " + BASE_URL);
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create()) // Sử dụng Gson để chuyển đổi JSON
                    .build();
        }
        return retrofit;
    }

    // Cập nhật URL cơ sở khi cần thiết
    public static void updateBaseUrl(String newUrl) {
        Log.d(TAG, "Cập nhật URL API từ " + BASE_URL + " sang " + newUrl);
        retrofit = new Retrofit.Builder()
                .baseUrl(newUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }
}