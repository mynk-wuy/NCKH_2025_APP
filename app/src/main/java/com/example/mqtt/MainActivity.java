package com.example.mqtt;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Menu;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.example.mqtt.model.SensorDataResponse;
import com.example.mqtt.model.User;
import com.example.mqtt.network.ApiClient;
import com.example.mqtt.network.ApiService;
import com.example.mqtt.ui.control.ControlFragment;
import com.example.mqtt.ui.dashboard.DashboardFragment;
import com.example.mqtt.ui.manager.AccountManagementFragment;
import com.example.mqtt.ui.manager.DeviceManagementFragment;
import com.example.mqtt.ui.notifications.NotificationsFragment;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mqtt.databinding.ActivityMainBinding;
import com.example.mqtt.connectMQTT.MqttManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private NavController navController;
    private MqttManager mqttManager; // Quản lý kết nối MQTT
    private Handler reconnectHandler = new Handler();
    private static final int RECONNECT_DELAY = 5000; // Thời gian thử kết nối lại sau 5 giây

    private SensorDataResponse sensorDataResponse; // Lưu dữ liệu cảm biến từ API
    private static final long REFRESH_INTERVAL = 30000; // Làm mới dữ liệu API mỗi 30 giây
    private Handler apiRefreshHandler = new Handler();
    private Runnable apiRefreshRunnable;

    private List<User> userData; // Lưu danh sách người dùng
    private Handler userRefreshHandler = new Handler();
    private static final long USER_REFRESH_INTERVAL = 30000; // Làm mới dữ liệu người dùng mỗi 30 giây
    private boolean isUserDataRefreshing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Khởi tạo MQTT Manager để kết nối với server MQTT
        mqttManager = MqttManager.getInstance(this);
        connectToMqtt();

        // Kiểm tra trạng thái đăng nhập
        SharedPreferences prefslogin = getSharedPreferences("user_prefs", MODE_PRIVATE);
        if (prefslogin.getInt("user_id", -1) == -1) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Thiết lập toolbar
        setSupportActionBar(binding.appBarMain.toolbar);
        binding.appBarMain.fab.setOnClickListener(view ->
                Snackbar.make(view, "Thay thế bằng hành động của bạn", Snackbar.LENGTH_LONG)
                        .setAction("Hành động", null)
                        .setAnchorView(R.id.fab).show());

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        // Kiểm tra roleId để phân quyền
        int roleId = prefslogin.getInt("role_id", 4); // Mặc định role là 4 (user)
        Log.d("ROLE_CHECK", "Role ID: " + roleId);

        // Cấu hình navigation dựa trên vai trò
        if (roleId == 4) { // Người dùng thông thường
            mAppBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.nav_home,
                    R.id.nav_dashboard,
                    R.id.nav_notifications)
                    .setOpenableLayout(drawer)
                    .build();
        } else { // Admin (role 1, 2, 3)
            mAppBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.nav_home,
                    R.id.nav_dashboard,
                    R.id.nav_notifications,
                    R.id.nav_control,
                    R.id.nav_manager,
                    R.id.nav_device_management)
                    .setOpenableLayout(drawer)
                    .build();
        }

        navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // Ẩn các mục menu không cần thiết dựa trên role
        Menu menu = navigationView.getMenu();
        MenuItem controlItem = menu.findItem(R.id.nav_control);
        if (controlItem != null) {
            controlItem.setVisible(roleId != 4);
        } else {
            Log.w("MainActivity", "Không tìm thấy nav_control");
        }
        MenuItem managerItem = menu.findItem(R.id.nav_manager);
        if (managerItem != null) {
            managerItem.setVisible(roleId != 4);
        } else {
            Log.w("MainActivity", "Không tìm thấy nav_manager");
        }
        MenuItem deviceManagementItem = menu.findItem(R.id.nav_device_management);
        if (deviceManagementItem != null) {
            deviceManagementItem.setVisible(roleId != 4);
        } else {
            Log.w("MainActivity", "Không tìm thấy nav_device_management");
        }

        // Xử lý sự kiện chọn menu
        navigationView.setNavigationItemSelectedListener(item -> {
            drawer.closeDrawer(navigationView); // Đóng drawer trước khi điều hướng

            if (item.getItemId() == R.id.nav_manager) {
                fetchUserData(); // Lấy dữ liệu người dùng khi chọn quản lý tài khoản
            } else if (item.getItemId() == R.id.nav_device_management) {
                refreshDeviceData(); // Làm mới dữ liệu thiết bị
            }

            // Xóa tab index dashboard nếu không phải dashboard
            if (item.getItemId() != R.id.nav_dashboard) {
                SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
                prefs.edit().remove("dashboard_tab_index").apply();
            }

            try {
                navController.navigate(item.getItemId()); // Thực hiện điều hướng
            } catch (IllegalArgumentException e) {
                Log.e("Navigation", "Mục điều hướng không hợp lệ: " + e.getMessage());
            }
            return true;
        });

        // Bắt đầu làm mới dữ liệu API và người dùng
        startApiRefreshing();
        startUserDataRefreshing();
    }

    // Bắt đầu làm mới dữ liệu từ API
    private void startApiRefreshing() {
        apiRefreshRunnable = new Runnable() {
            @Override
            public void run() {
                fetchApiData();
                apiRefreshHandler.postDelayed(this, REFRESH_INTERVAL);
            }
        };
        apiRefreshHandler.post(apiRefreshRunnable);
    }

    // Bắt đầu làm mới dữ liệu người dùng
    private void startUserDataRefreshing() {
        if (!isUserDataRefreshing) {
            isUserDataRefreshing = true;
            userRefreshHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    fetchUserData();
                    userRefreshHandler.postDelayed(this, USER_REFRESH_INTERVAL);
                }
            }, 0);
        }
    }

    // Lấy dữ liệu cảm biến từ API
    private void fetchApiData() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getSensorData().enqueue(new Callback<SensorDataResponse>() {
            @Override
            public void onResponse(Call<SensorDataResponse> call, Response<SensorDataResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    sensorDataResponse = response.body();
                    notifyFragmentsAboutData(sensorDataResponse); // Thông báo dữ liệu mới cho fragment
                }
            }

            @Override
            public void onFailure(Call<SensorDataResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Lỗi kết nối API: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Lấy danh sách người dùng từ API
    public void fetchUserData() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getQuanLyUsers().enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    userData = response.body();
                    notifyFragmentsAboutUserData(userData); // Thông báo danh sách người dùng
                } else {
                    Log.e("API_ERROR", "Lỗi phản hồi: " + response.code());
                    notifyFragmentsAboutUserData(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                Log.e("API_ERROR", "Không thể lấy người dùng", t);
                notifyFragmentsAboutUserData(new ArrayList<>());
            }
        });
    }

    // Thông báo dữ liệu cảm biến cho DashboardFragment
    private void notifyFragmentsAboutData(SensorDataResponse data) {
        Fragment navHostFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
        if (navHostFragment != null) {
            Fragment currentFragment = navHostFragment.getChildFragmentManager().getPrimaryNavigationFragment();
            if (currentFragment instanceof DashboardFragment) {
                ((DashboardFragment) currentFragment).updateData(data);
            }
        }
    }

    // Thông báo dữ liệu người dùng cho AccountManagementFragment
    private void notifyFragmentsAboutUserData(List<User> data) {
        Fragment navHostFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
        if (navHostFragment == null) {
            return;
        }
        Fragment currentFragment = navHostFragment.getChildFragmentManager().getPrimaryNavigationFragment();
        if (currentFragment instanceof AccountManagementFragment) {
            ((AccountManagementFragment) currentFragment).updateUserList(data);
        }
    }

    // Kết nối với server MQTT
    private void connectToMqtt() {
        mqttManager.connect(connected -> {
            runOnUiThread(() -> {
                if (connected) {
                    Toast.makeText(this, "Đã kết nối MQTT", Toast.LENGTH_SHORT).show();
                    notifyFragmentsAboutConnection(true);
                } else {
                    Toast.makeText(this, "Lỗi kết nối MQTT, thử lại sau...", Toast.LENGTH_SHORT).show();
                    reconnectHandler.postDelayed(this::connectToMqtt, RECONNECT_DELAY);
                }
            });
        });
    }

    // Thông báo trạng thái kết nối MQTT cho các fragment
    private void notifyFragmentsAboutConnection(boolean isConnected) {
        Fragment navHostFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
        if (navHostFragment != null) {
            Fragment currentFragment = navHostFragment.getChildFragmentManager().getPrimaryNavigationFragment();
            if (currentFragment instanceof ControlFragment && isConnected) {
                ((ControlFragment) currentFragment).onMqttConnected();
            } else if (currentFragment instanceof NotificationsFragment && isConnected) {
                ((NotificationsFragment) currentFragment).subscribeToTopics();
            }
        }
    }

    // Hủy các handler khi activity bị hủy
    @Override
    protected void onDestroy() {
        super.onDestroy();
        reconnectHandler.removeCallbacksAndMessages(null);
        apiRefreshHandler.removeCallbacksAndMessages(null);
        userRefreshHandler.removeCallbacksAndMessages(null);
    }

    // Tạo menu tùy chọn
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    // Xử lý nút quay lại trên toolbar
    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    // Kiểm tra trạng thái kết nối MQTT
    public boolean isMqttConnected() {
        return mqttManager != null && mqttManager.isConnected();
    }

    // Trả về MqttManager
    public MqttManager getMqttManager() {
        return mqttManager;
    }

    // Trả về dữ liệu cảm biến
    public SensorDataResponse getSensorData() {
        return sensorDataResponse;
    }

    // Trả về danh sách người dùng
    public List<User> getUserData() {
        return userData != null ? userData : new ArrayList<>();
    }

    // Làm mới dữ liệu người dùng
    public void refreshUserData() {
        fetchUserData();
    }

    // Làm mới dữ liệu thiết bị
    public void refreshDeviceData() {
        Fragment navHostFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
        if (navHostFragment != null) {
            Fragment currentFragment = navHostFragment.getChildFragmentManager().getPrimaryNavigationFragment();
            if (currentFragment instanceof DeviceManagementFragment) {
                ((DeviceManagementFragment) currentFragment).loadDevices();
            }
        }
    }

    // Xử lý sự kiện chọn mục menu
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            showLogoutConfirmation(); // Hiển thị xác nhận đăng xuất
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Hiển thị hộp thoại xác nhận đăng xuất
    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất không?")
                .setPositiveButton("Có", (dialog, which) -> {
                    performLogout(); // Thực hiện đăng xuất
                })
                .setNegativeButton("Không", null)
                .setIcon(R.drawable.warning_24px)
                .show();
    }

    // Thực hiện đăng xuất
    private void performLogout() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        prefs.edit().clear().apply(); // Xóa thông tin người dùng

        if (mqttManager != null && mqttManager.isConnected()) {
            mqttManager.disconnect(); // Ngắt kết nối MQTT
        }

        apiRefreshHandler.removeCallbacksAndMessages(null);
        userRefreshHandler.removeCallbacksAndMessages(null);
        reconnectHandler.removeCallbacksAndMessages(null);

        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();

        Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
    }
}