package com.example.mqtt.network;

import com.example.mqtt.model.Device;
import com.example.mqtt.model.SensorDataResponse;
import com.example.mqtt.model.User;
import com.example.mqtt.model.UserResponse;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface ApiService {

    // Lấy dữ liệu cảm biến
    @GET("api/sensor/data")
    Call<SensorDataResponse> getSensorData();

    // --- API cho quản lý thiết bị ---
    // Lấy danh sách thiết bị
    @Headers({
            "ngrok-skip-browser-warning: true",
            "Content-Type: application/json"
    })
    @GET("api/devices")
    Call<ResponseBody> getDevices();

    // Thêm thiết bị mới
    @Headers({
            "ngrok-skip-browser-warning: true",
            "Content-Type: application/json"
    })
    @POST("api/devices")
    Call<ResponseBody> addDevice(@Body Device device);

    // Cập nhật thông tin thiết bị
    @Headers({
            "ngrok-skip-browser-warning: true",
            "Content-Type: application/json"
    })
    @PUT("api/devices/{sensor_id}")
    Call<ResponseBody> updateDevice(
            @Path("sensor_id") String sensorId,
            @Body Device device
    );

    // Xóa thiết bị
    @Headers({
            "ngrok-skip-browser-warning: true",
            "Content-Type: application/json"
    })
    @DELETE("api/devices/{sensor_id}")
    Call<ResponseBody> deleteDevice(
            @Path("sensor_id") String sensorId
    );

    // --- API cho quản lý người dùng ---
    // Lấy danh sách người dùng
    @Headers({
            "ngrok-skip-browser-warning: true",
            "Content-Type: application/json"
    })
    @GET("api/quanlyusers")
    Call<List<User>> getQuanLyUsers();

    // Tạo người dùng mới
    @Headers({
            "ngrok-skip-browser-warning: true",
            "Content-Type: application/json"
    })
    @POST("api/quanlyusers")
    Call<ResponseBody> createQuanLyUser(@Body User user);

    // Cập nhật thông tin người dùng
    @Headers({
            "ngrok-skip-browser-warning: true",
            "Content-Type: application/json"
    })
    @PUT("api/quanlyusers/{user_id}")
    Call<ResponseBody> updateQuanLyUser(
            @Path("user_id") int userId,
            @Body User user
    );

    // Xóa người dùng
    @Headers({
            "ngrok-skip-browser-warning: true",
            "Content-Type: application/json"
    })
    @DELETE("api/quanlyusers/{user_id}")
    Call<ResponseBody> deleteQuanLyUser(
            @Path("user_id") int userId
    );

    // --- API cho đăng nhập ---
    // Lấy danh sách người dùng để xác thực đăng nhập
    @Headers({
            "ngrok-skip-browser-warning: true",
            "Content-Type: application/json"
    })
    @GET("api/users")
    Call<UserResponse> getUsersForLogin();
}