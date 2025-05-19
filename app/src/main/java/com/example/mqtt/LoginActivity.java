package com.example.mqtt;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mqtt.databinding.ActivityLoginBinding;
import com.example.mqtt.model.User;
import com.example.mqtt.model.UserResponse;
import com.example.mqtt.network.ApiClient;
import com.example.mqtt.network.ApiService;
import com.github.mikephil.charting.BuildConfig;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private ActivityLoginBinding binding;
    private ApiService apiService;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Bật StrictMode trong chế độ debug để phát hiện lỗi
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
        }

        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Khởi tạo SharedPreferences và ApiService
        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        apiService = ApiClient.getClient().create(ApiService.class);

        // Xử lý sự kiện nút đăng nhập
        binding.btnLogin.setOnClickListener(v -> {
            String email = binding.edtEmail.getText().toString().trim();
            String password = binding.edtPassword.getText().toString().trim();

            if (validateLogin(email, password)) {
                authenticateUser(email, password); // Xác thực người dùng
            } else {
                Toast.makeText(this, "Vui lòng nhập email và mật khẩu", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Ghi log khi cửa sổ thay đổi focus
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        Log.d(TAG, "onWindowFocusChanged: hasFocus=" + hasFocus);
    }

    // Kiểm tra email và mật khẩu có trống không
    private boolean validateLogin(String email, String password) {
        return !email.isEmpty() && !password.isEmpty();
    }

    // Kiểm tra kết nối mạng
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    // Xác thực người dùng qua API
    private void authenticateUser(String email, String password) {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "Không có kết nối mạng. Vui lòng kiểm tra lại.", Toast.LENGTH_LONG).show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnLogin.setEnabled(false);

        Call<UserResponse> call = apiService.getUsersForLogin();
        call.enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                // Xử lý dữ liệu trong luồng nền
                new ProcessUserResponseTask(email, password).execute(response);
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnLogin.setEnabled(true);
                String errorMessage = "Lỗi kết nối: " + t.getMessage();
                if (t instanceof java.net.UnknownHostException) {
                    errorMessage = "Không thể kết nối đến server. Vui lòng kiểm tra URL server.";
                } else if (t instanceof java.net.SocketTimeoutException) {
                    errorMessage = "Hết thời gian chờ. Vui lòng thử lại.";
                }
                Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                Log.e(TAG, "Lỗi API call: " + t.getMessage(), t);
            }
        });
    }

    // AsyncTask xử lý phản hồi API
    private class ProcessUserResponseTask extends AsyncTask<Response<UserResponse>, Void, String> {
        private String email;
        private String password;
        private User loginUser;

        ProcessUserResponseTask(String email, String password) {
            this.email = email;
            this.password = password;
        }

        @Override
        protected String doInBackground(Response<UserResponse>... responses) {
            Response<UserResponse> response = responses[0];
            if (response.isSuccessful() && response.body() != null) {
                UserResponse userResponse = response.body();
                Log.d(TAG, "API response: count=" + userResponse.getCount() +
                        ", success=" + userResponse.isSuccess() +
                        ", users=" + userResponse.getUsers().size());

                if (userResponse.getUsers() != null && !userResponse.getUsers().isEmpty()) {
                    // Tạo HashMap để tra cứu email nhanh
                    Map<String, User> userMap = new HashMap<>();
                    for (User user : userResponse.getUsers()) {
                        if (user.getEmail() != null) {
                            userMap.put(user.getEmail().toLowerCase(), user);
                        }
                    }

                    User user = userMap.get(email.toLowerCase());
                    if (user != null) {
                        if (user.getPassword() != null && user.getPassword().equals(password)) {
                            loginUser = UserResponse.convertToLoginUser(user);
                            return null; // Đăng nhập thành công
                        } else {
                            return "Mật khẩu không đúng";
                        }
                    } else {
                        return "Email không tồn tại";
                    }
                } else {
                    return "Dữ liệu người dùng trống";
                }
            } else {
                String errorMessage = "Lỗi đăng nhập: Mã lỗi " + response.code();
                try {
                    if (response.errorBody() != null) {
                        errorMessage += " - " + response.errorBody().string();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Lỗi đọc error body", e);
                }
                return errorMessage;
            }
        }

        @Override
        protected void onPostExecute(String errorMessage) {
            binding.progressBar.setVisibility(View.GONE);
            binding.btnLogin.setEnabled(true);

            if (errorMessage == null && loginUser != null) {
                Log.d(TAG, "User data before saving: ID=" + loginUser.getUserId() +
                        ", Username=" + loginUser.getUsername() +
                        ", Email=" + loginUser.getEmail() +
                        ", RoleId=" + loginUser.getRoleId());
                saveUserData(loginUser); // Lưu thông tin người dùng
                redirectToMainActivity(); // Chuyển đến MainActivity
            } else {
                Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                Log.e(TAG, "Lỗi response: " + errorMessage);
            }
        }
    }

    // Lưu thông tin người dùng vào SharedPreferences
    private void saveUserData(User user) {
        if (user == null) {
            Log.e(TAG, "Đối tượng User là null");
            Toast.makeText(this, "Lỗi hệ thống: Dữ liệu người dùng không hợp lệ", Toast.LENGTH_LONG).show();
            return;
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("user_id", user.getUserId());
        editor.putString("username", user.getUsername() != null ? user.getUsername() : "");
        editor.putString("email", user.getEmail() != null ? user.getEmail() : "");
        editor.putInt("role_id", user.getRoleId());
        editor.putString("role_name", user.getRoleName() != null ? user.getRoleName() : "");
        editor.apply();

        Log.d(TAG, "Đã lưu dữ liệu: user_id=" + user.getUserId() + ", role_id=" + user.getRoleId());
    }

    // Chuyển đến MainActivity sau khi đăng nhập
    private void redirectToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}