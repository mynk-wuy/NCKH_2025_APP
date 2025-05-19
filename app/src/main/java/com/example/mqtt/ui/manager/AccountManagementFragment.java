package com.example.mqtt.ui.manager;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mqtt.R;
import com.example.mqtt.model.User;
import com.example.mqtt.network.ApiClient;
import com.example.mqtt.network.ApiService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AccountManagementFragment extends Fragment {
    private static final String TAG = "AccountManagement";
    private static final long REFRESH_INTERVAL = 20000; // Làm mới mỗi 20 giây

    private ListView listViewAccounts; // Hiển thị danh sách tài khoản
    private Button btnBack, btnAdd; // Nút quay lại và thêm tài khoản
    private UserAdapter adapter;
    private List<User> userList; // Danh sách người dùng
    private ApiService apiService;
    private ProgressDialog progressDialog;
    private Handler refreshHandler = new Handler();
    private boolean isRefreshing = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        apiService = ApiClient.getClient().create(ApiService.class); // Khởi tạo ApiService
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account_management, container, false);

        // Khởi tạo các thành phần giao diện
        listViewAccounts = view.findViewById(R.id.listViewAccounts);
        btnBack = view.findViewById(R.id.btnBack);
        btnAdd = view.findViewById(R.id.btnAdd);

        userList = new ArrayList<>();
        adapter = new UserAdapter(requireContext(), userList);
        listViewAccounts.setAdapter(adapter);

        // Xử lý sự kiện nút
        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());
        btnAdd.setOnClickListener(v -> showAddUserDialog());

        loadUsers(); // Tải danh sách người dùng
        startAutoRefresh(); // Bắt đầu làm mới tự động

        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        stopAutoRefresh(); // Dừng làm mới khi fragment tạm dừng
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUsers();
        startAutoRefresh();
    }

    // Kiểm tra kết nối mạng
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    // Bắt đầu làm mới tự động
    private void startAutoRefresh() {
        if (!isRefreshing) {
            isRefreshing = true;
            refreshHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    loadUsers();
                    refreshHandler.postDelayed(this, REFRESH_INTERVAL);
                }
            }, REFRESH_INTERVAL);
        }
    }

    // Dừng làm mới tự động
    private void stopAutoRefresh() {
        isRefreshing = false;
        refreshHandler.removeCallbacksAndMessages(null);
    }

    // Tải danh sách người dùng từ API
    private void loadUsers() {
        if (!isNetworkAvailable()) {
            showError("Không có kết nối mạng. Vui lòng kiểm tra lại.");
            return;
        }

        Log.d(TAG, "Đang tải danh sách người dùng...");
        showProgress("Đang tải dữ liệu...");

        apiService.getQuanLyUsers().enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                dismissProgress();
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "API response: users=" + response.body().size());
                    updateUserList(response.body()); // Cập nhật danh sách
                } else {
                    handleErrorResponse(response);
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                dismissProgress();
                String errorMessage = "Lỗi kết nối: " + t.getMessage();
                if (t instanceof java.net.UnknownHostException) {
                    errorMessage = "Không thể kết nối đến server.";
                } else if (t instanceof java.net.SocketTimeoutException) {
                    errorMessage = "Hết thời gian chờ.";
                }
                Log.e(TAG, errorMessage, t);
                showError(errorMessage);
            }
        });
    }

    // Cập nhật danh sách người dùng lên giao diện
    public void updateUserList(List<User> users) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> {
            try {
                if (users == null) {
                    Log.w(TAG, "Dữ liệu người dùng null");
                    showError("Dữ liệu người dùng trống");
                    return;
                }
                userList.clear();
                userList.addAll(users);
                adapter.notifyDataSetChanged();
                Log.d(TAG, "Đã cập nhật " + userList.size() + " người dùng");
            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi cập nhật dữ liệu", e);
                showError("Lỗi hệ thống: " + e.getMessage());
            }
        });
    }

    // Hiển thị hộp thoại thêm tài khoản
    private void showAddUserDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Thêm tài khoản mới");
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_user, null);
        builder.setView(dialogView);

        EditText etUsername = dialogView.findViewById(R.id.etUsername);
        EditText etEmail = dialogView.findViewById(R.id.etEmail);
        EditText etPassword = dialogView.findViewById(R.id.etPassword);
        EditText etRole = dialogView.findViewById(R.id.etRole);
        TextView tvRoleInfo = dialogView.findViewById(R.id.tvRoleInfo);
        tvRoleInfo.setText("Vai trò:\n1: Super Admin\n2: Admin trạm 1\n3: Admin trạm 2\n4: User");
        tvRoleInfo.setVisibility(View.VISIBLE);

        builder.setPositiveButton("Thêm", (dialog, which) -> {
            String username = etUsername.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String roleStr = etRole.getText().toString().trim();

            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || roleStr.isEmpty()) {
                showError("Vui lòng điền đầy đủ thông tin");
                return;
            }

            try {
                int roleId = Integer.parseInt(roleStr);
                if (roleId < 1 || roleId > 4) {
                    showError("Vai trò chỉ được từ 1-4");
                    return;
                }

                User newUser = new User();
                newUser.setUsername(username);
                newUser.setEmail(email);
                newUser.setPassword(password);
                newUser.setRoleId(roleId);

                Log.d(TAG, "Thêm người dùng: username=" + username + ", email=" + email);

                showProgress("Đang thêm tài khoản...");
                apiService.createQuanLyUser(newUser).enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        dismissProgress();
                        try {
                            if (response.isSuccessful()) {
                                showSuccess("Thêm tài khoản thành công!");
                                loadUsers();
                            } else {
                                String errorBody = response.errorBody() != null ?
                                        response.errorBody().string() : "Lỗi không xác định";
                                Log.e(TAG, "Lỗi thêm tài khoản: " + errorBody);
                                showError("Lỗi thêm tài khoản: " + errorBody);
                            }
                        } catch (IOException e) {
                            Log.e(TAG, "Lỗi đọc error body", e);
                            showError("Lỗi hệ thống khi thêm tài khoản");
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        dismissProgress();
                        String errorMessage = "Lỗi kết nối: " + t.getMessage();
                        if (t instanceof java.net.UnknownHostException) {
                            errorMessage = "Không thể kết nối đến server.";
                        } else if (t instanceof java.net.SocketTimeoutException) {
                            errorMessage = "Hết thời gian chờ.";
                        }
                        Log.e(TAG, errorMessage, t);
                        showError(errorMessage);
                    }
                });
            } catch (NumberFormatException e) {
                showError("Vai trò phải là số từ 1-4");
            }
        });

        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    // Hiển thị hộp thoại chỉnh sửa tài khoản
    private void showEditUserDialog(User user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Sửa tài khoản: " + (user.getUsername() != null ? user.getUsername() : "Không xác định"));
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_user, null);
        builder.setView(dialogView);

        EditText etUsername = dialogView.findViewById(R.id.etUsername);
        EditText etEmail = dialogView.findViewById(R.id.etEmail);
        EditText etPassword = dialogView.findViewById(R.id.etPassword);
        EditText etRole = dialogView.findViewById(R.id.etRole);
        TextView tvRoleInfo = dialogView.findViewById(R.id.tvRoleInfo);

        etUsername.setText(user.getUsername() != null ? user.getUsername() : "");
        etEmail.setText(user.getEmail() != null ? user.getEmail() : "");
        etRole.setText(String.valueOf(user.getRoleId()));
        tvRoleInfo.setText("Vai trò:\n1: Super Admin\n2: Admin trạm 1\n3: Admin trạm 2\n4: User");
        tvRoleInfo.setVisibility(View.VISIBLE);

        builder.setPositiveButton("Lưu", (dialog, which) -> {
            String username = etUsername.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String roleStr = etRole.getText().toString().trim();

            if (username.isEmpty() || email.isEmpty() || roleStr.isEmpty()) {
                showError("Vui lòng điền đầy đủ thông tin (mật khẩu có thể để trống)");
                return;
            }

            try {
                int roleId = Integer.parseInt(roleStr);
                if (roleId < 1 || roleId > 4) {
                    showError("Vai trò chỉ được từ 1-4");
                    return;
                }

                User updatedUser = new User();
                updatedUser.setUsername(username);
                updatedUser.setEmail(email);
                updatedUser.setRoleId(roleId);

                String finalPassword = !password.isEmpty() ? password : user.getPassword();
                if (finalPassword == null) {
                    showError("Lỗi: Không thể lấy mật khẩu hiện tại. Vui lòng nhập mật khẩu mới.");
                    return;
                }
                updatedUser.setPassword(finalPassword);

                Log.d(TAG, "Cập nhật người dùng: username=" + username + ", email=" + email);

                showProgress("Đang cập nhật tài khoản...");
                apiService.updateQuanLyUser(user.getUserId(), updatedUser).enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        dismissProgress();
                        try {
                            if (response.isSuccessful()) {
                                showSuccess("Cập nhật tài khoản thành công!");
                                loadUsers();
                            } else {
                                String errorBody = response.errorBody() != null ?
                                        response.errorBody().string() : "Lỗi không xác định";
                                Log.e(TAG, "Lỗi cập nhật tài khoản: " + errorBody);
                                showError("Lỗi cập nhật tài khoản: " + errorBody);
                            }
                        } catch (IOException e) {
                            Log.e(TAG, "Lỗi đọc error body", e);
                            showError("Lỗi hệ thống khi cập nhật tài khoản");
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        dismissProgress();
                        String errorMessage = "Lỗi kết nối: " + t.getMessage();
                        if (t instanceof java.net.UnknownHostException) {
                            errorMessage = "Không thể kết nối đến server.";
                        } else if (t instanceof java.net.SocketTimeoutException) {
                            errorMessage = "Hết thời gian chờ.";
                        }
                        Log.e(TAG, errorMessage, t);
                        showError(errorMessage);
                    }
                });
            } catch (NumberFormatException e) {
                showError("Vai trò phải là số từ 1-4");
            }
        });

        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    // Xóa tài khoản
    private void deleteUser(int userId) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn chắc chắn muốn xóa tài khoản này?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    if (!isNetworkAvailable()) {
                        showError("Không có kết nối mạng. Vui lòng kiểm tra lại.");
                        return;
                    }

                    showProgress("Đang xóa tài khoản...");
                    apiService.deleteQuanLyUser(userId).enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            dismissProgress();
                            try {
                                if (response.isSuccessful()) {
                                    showSuccess("Xóa tài khoản thành công!");
                                    loadUsers();
                                } else {
                                    String errorBody = response.errorBody() != null ?
                                            response.errorBody().string() : "Lỗi không xác định";
                                    Log.e(TAG, "Lỗi xóa tài khoản: " + errorBody);
                                    showError("Lỗi xóa tài khoản: " + errorBody);
                                }
                            } catch (IOException e) {
                                Log.e(TAG, "Lỗi đọc error body", e);
                                showError("Lỗi hệ thống khi xóa tài khoản");
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            dismissProgress();
                            String errorMessage = "Lỗi kết nối: " + t.getMessage();
                            if (t instanceof java.net.UnknownHostException) {
                                errorMessage = "Không thể kết nối đến server.";
                            } else if (t instanceof java.net.SocketTimeoutException) {
                                errorMessage = "Hết thời gian chờ.";
                            }
                            Log.e(TAG, errorMessage, t);
                            showError(errorMessage);
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    // Hiển thị tiến trình
    private void showProgress(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(requireContext());
            progressDialog.setCancelable(false);
        }
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    // Ẩn tiến trình
    private void dismissProgress() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    // Hiển thị thông báo thành công
    private void showSuccess(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    // Hiển thị thông báo lỗi
    private void showError(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
    }

    // Xử lý phản hồi lỗi từ API
    private void handleErrorResponse(Response<?> response) {
        try {
            String errorBody = response.errorBody() != null ?
                    response.errorBody().string() : "Lỗi không xác định";
            Log.e(TAG, "Lỗi API: " + response.code() + " - " + errorBody);
            showError("Lỗi: " + response.code() + " - " + errorBody);
        } catch (IOException e) {
            Log.e(TAG, "Lỗi đọc error body", e);
            showError("Lỗi hệ thống");
        }
    }

    // Adapter hiển thị danh sách người dùng
    class UserAdapter extends ArrayAdapter<User> {
        public UserAdapter(Context context, List<User> users) {
            super(context, 0, users);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            User user = getItem(position);
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_user, parent, false);
            }

            TextView tvUsername = convertView.findViewById(R.id.tvUsername);
            TextView tvEmail = convertView.findViewById(R.id.tvEmail);
            TextView tvRole = convertView.findViewById(R.id.tvRole);
            Button btnEdit = convertView.findViewById(R.id.btnEdit);
            Button btnDelete = convertView.findViewById(R.id.btnDelete);

            if (user != null) {
                tvUsername.setText("Tài khoản: " + (user.getUsername() != null ? user.getUsername() : "Không xác định"));
                tvEmail.setText("Email: " + (user.getEmail() != null ? user.getEmail() : "Không xác định"));
                tvRole.setText("Vai trò: " + (user.getRoleName() != null ? user.getRoleName() : "Không xác định"));

                btnEdit.setOnClickListener(v -> showEditUserDialog(user));
                btnDelete.setOnClickListener(v -> deleteUser(user.getUserId()));
            } else {
                Log.w(TAG, "Người dùng tại vị trí " + position + " là null");
            }

            return convertView;
        }
    }

    // Hủy các tài nguyên khi fragment bị hủy
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopAutoRefresh();
        dismissProgress();
    }
}