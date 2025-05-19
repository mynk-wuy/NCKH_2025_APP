package com.example.mqtt.ui.notifications;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mqtt.MainActivity;
import com.example.mqtt.R;
import com.example.mqtt.connectMQTT.MqttConfig;
import com.example.mqtt.connectMQTT.MqttManager;
import com.example.mqtt.databinding.FragmentNotificationsBinding;
import com.example.mqtt.model.Device;
import com.example.mqtt.network.ApiClient;
import com.example.mqtt.network.ApiService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationsFragment extends Fragment {
    private FragmentNotificationsBinding binding;
    private MqttManager mqttManager; // Quản lý kết nối MQTT
    private TextView tvCO, tvH2, tvNH3, tvCO2, tvH22, tvNH32; // Hiển thị giá trị cảm biến
    private TextView tvCOThreshold, tvH2Threshold, tvNH3Threshold; // Hiển thị ngưỡng cảm biến Trạm 1
    private TextView tvCOThreshold2, tvH2Threshold2, tvNH3Threshold2; // Hiển thị ngưỡng cảm biến Trạm 2
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean isFragmentActive = false; // Trạng thái fragment
    private int roleId; // Vai trò người dùng
    private Map<String, Float> thresholdMap = new HashMap<>(); // Lưu ngưỡng cảm biến

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false);

        // Khởi tạo TabHost để hiển thị Trạm 1 và Trạm 2
        TabHost noti = binding.noti;
        noti.setup();

        // Lấy roleId từ SharedPreferences
        SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        roleId = prefs.getInt("role_id", 4); // Mặc định là 4 (user)

        // Thêm tab dựa trên vai trò
        if (roleId == 2) {
            addTab1(noti); // Chỉ thêm Tab Trạm 1
        } else if (roleId == 3) {
            addTab2(noti); // Chỉ thêm Tab Trạm 2
        } else {
            addTab1(noti); // Thêm cả hai tab cho role 1 và 4
            addTab2(noti);
        }

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        isFragmentActive = true;

        // Lấy MqttManager từ MainActivity
        MainActivity mainActivity = (MainActivity) requireActivity();
        mqttManager = mainActivity.getMqttManager();

        // Khởi tạo TextView hiển thị giá trị và ngưỡng
        tvCO = view.findViewById(R.id.tvCO);
        tvH2 = view.findViewById(R.id.tvH2);
        tvNH3 = view.findViewById(R.id.tvNH3);
        tvCO2 = view.findViewById(R.id.tvCO_2);
        tvH22 = view.findViewById(R.id.tvH2_2);
        tvNH32 = view.findViewById(R.id.tvNH3_2);
        tvCOThreshold = view.findViewById(R.id.tvCOThreshold);
        tvH2Threshold = view.findViewById(R.id.tvH2Threshold);
        tvNH3Threshold = view.findViewById(R.id.tvNH3Threshold);
        tvCOThreshold2 = view.findViewById(R.id.tvCOThreshold_2);
        tvH2Threshold2 = view.findViewById(R.id.tvH2Threshold_2);
        tvNH3Threshold2 = view.findViewById(R.id.tvNH3Threshold_2);

        // Ẩn nội dung tab không cần thiết theo role
        if (roleId == 2) {
            hideTabContent(view, R.id.tabcb2); // Ẩn Tab Trạm 2
        } else if (roleId == 3) {
            hideTabContent(view, R.id.tabcb1); // Ẩn Tab Trạm 1
        }

        resetValues(); // Đặt giá trị mặc định cho TextView
        fetchThresholdData(); // Lấy dữ liệu ngưỡng từ API

        // Đăng ký topic nếu đã kết nối MQTT
        if (mainActivity.isMqttConnected()) {
            subscribeToTopics();
        }
    }

    // Ẩn nội dung tab
    private void hideTabContent(View view, int tabContentId) {
        View tabContent = view.findViewById(tabContentId);
        if (tabContent != null) {
            tabContent.setVisibility(View.GONE);
        }
    }

    // Đặt giá trị mặc định cho TextView
    private void resetValues() {
        if (roleId != 3) { // Tab Trạm 1
            tvCO.setText("--");
            tvH2.setText("--");
            tvNH3.setText("--");
            tvCOThreshold.setText("--");
            tvH2Threshold.setText("--");
            tvNH3Threshold.setText("--");
        }
        if (roleId != 2) { // Tab Trạm 2
            tvCO2.setText("--");
            tvH22.setText("--");
            tvNH32.setText("--");
            tvCOThreshold2.setText("--");
            tvH2Threshold2.setText("--");
            tvNH3Threshold2.setText("--");
        }
    }

    // Lấy dữ liệu ngưỡng từ API
    private void fetchThresholdData() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<ResponseBody> call = apiService.getDevices();
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseData = response.body().string();
                        JSONArray jsonArray = new JSONArray(responseData);
                        Type listType = new TypeToken<List<Device>>(){}.getType();
                        List<Device> devices = new Gson().fromJson(jsonArray.toString(), listType);

                        for (Device device : devices) {
                            thresholdMap.put(device.getSensorId(), device.getThreshold()); // Lưu ngưỡng
                            updateThresholdUI(device); // Cập nhật giao diện ngưỡng
                        }
                    } catch (Exception e) {
                        Log.e("Notifications", "Lỗi phân tích dữ liệu ngưỡng", e);
                        showToast("Lỗi khi xử lý dữ liệu ngưỡng");
                    }
                } else {
                    showToast("Không thể lấy dữ liệu ngưỡng");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("Notifications", "Không thể lấy dữ liệu ngưỡng", t);
                showToast("Lỗi kết nối khi lấy dữ liệu ngưỡng");
            }
        });
    }

    // Cập nhật giao diện ngưỡng
    private void updateThresholdUI(Device device) {
        String sensorId = device.getSensorId();
        Float threshold = device.getThreshold();
        if (threshold == null) return;

        if (sensorId.startsWith("Lab001") && roleId != 3) { // Trạm 1
            switch (sensorId) {
                case "Lab001CO":
                    tvCOThreshold.setText(String.valueOf(threshold));
                    break;
                case "Lab001H2":
                    tvH2Threshold.setText(String.valueOf(threshold));
                    break;
                case "Lab001NH3":
                    tvNH3Threshold.setText(String.valueOf(threshold));
                    break;
            }
        } else if (sensorId.startsWith("A4002") && roleId != 2) { // Trạm 2
            switch (sensorId) {
                case "A4002CO":
                    tvCOThreshold2.setText(String.valueOf(threshold));
                    break;
                case "A4002H2":
                    tvH2Threshold2.setText(String.valueOf(threshold));
                    break;
                case "A4002NH3":
                    tvNH3Threshold2.setText(String.valueOf(threshold));
                    break;
            }
        }
    }

    // Đăng ký các topic MQTT
    public void subscribeToTopics() {
        if (mqttManager != null && isAdded()) {
            if (roleId != 3) { // Trạm 1
                mqttManager.subscribe(MqttConfig.TOPIC_SENSOR_DATA1, 1, this::handleSensorData);
            }
            if (roleId != 2) { // Trạm 2
                mqttManager.subscribe(MqttConfig.TOPIC_SENSOR_DATA2, 1, this::handleSensorData);
            }
        }
    }

    // Xử lý dữ liệu cảm biến từ MQTT
    private void handleSensorData(String message) {
        if (!isFragmentActive || !isAdded()) return;
        try {
            JSONObject json = new JSONObject(message);
            handler.post(() -> {
                if (!isFragmentActive || !isAdded() || getActivity() == null) return;
                try {
                    if (json.has("A4002CO") && roleId != 2) { // Trạm 2
                        int co = json.getInt("A4002CO");
                        int h2 = json.getInt("A4002H2");
                        int nh3 = json.getInt("A4002NH3");
                        tvCO2.setText(String.valueOf(co));
                        tvH22.setText(String.valueOf(h2));
                        tvNH32.setText(String.valueOf(nh3));
                        checkThreshold("A4002CO", co);
                        checkThreshold("A4002H2", h2);
                        checkThreshold("A4002NH3", nh3);
                    }
                    if (json.has("Lab001CO") && roleId != 3) { // Trạm 1
                        int co = json.getInt("Lab001CO");
                        int h2 = json.getInt("Lab001H2");
                        int nh3 = json.getInt("Lab001NH3");
                        tvCO.setText(String.valueOf(co));
                        tvH2.setText(String.valueOf(h2));
                        tvNH3.setText(String.valueOf(nh3));
                        checkThreshold("Lab001CO", co);
                        checkThreshold("Lab001H2", h2);
                        checkThreshold("Lab001NH3", nh3);
                    }
                } catch (JSONException e) {
                    Log.e("Notifications", "Lỗi dữ liệu cảm biến", e);
                    showToast("Lỗi dữ liệu cảm biến");
                }
            });
        } catch (JSONException e) {
            Log.e("Notifications", "Lỗi phân tích JSON", e);
            showToast("Lỗi phân tích dữ liệu");
        }
    }

    // Kiểm tra ngưỡng và hiển thị cảnh báo
    private void checkThreshold(String sensorId, int value) {
        Float threshold = thresholdMap.get(sensorId);
        if (threshold != null && value > threshold) {
            String gasName = getGasName(sensorId);
            String stationName = sensorId.startsWith("Lab001") ? "Trạm 1" : "Trạm 2";
            String alertMessage = String.format("Cảnh báo: %s - %s vượt ngưỡng (%d > %.0f)",
                    stationName, gasName, value, threshold);
            showAlert(alertMessage);
        }
    }

    // Lấy tên khí từ sensorId
    private String getGasName(String sensorId) {
        if (sensorId.endsWith("CO")) return "CO";
        if (sensorId.endsWith("H2")) return "H2";
        if (sensorId.endsWith("NH3")) return "NH3";
        return "Khí";
    }

    // Hiển thị thông báo Toast
    private void showToast(String message) {
        if (isAdded() && getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    // Hiển thị cảnh báo
    private void showAlert(String message) {
        if (isAdded() && getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        }
    }

    // Thêm Tab Trạm 1
    private void addTab1(TabHost noti) {
        TabHost.TabSpec notitram1 = noti.newTabSpec("noti1");
        notitram1.setContent(R.id.tabcb1);
        notitram1.setIndicator("Trạm 1");
        noti.addTab(notitram1);
    }

    // Thêm Tab Trạm 2
    private void addTab2(TabHost noti) {
        TabHost.TabSpec notitram2 = noti.newTabSpec("noti2");
        notitram2.setContent(R.id.tabcb2);
        notitram2.setIndicator("Trạm 2");
        noti.addTab(notitram2);
    }

    @Override
    public void onResume() {
        super.onResume();
        isFragmentActive = true;
        if (mqttManager != null && mqttManager.isConnected()) {
            subscribeToTopics();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        isFragmentActive = false;
    }

    // Hủy các callback và hủy đăng ký topic khi fragment bị hủy
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isFragmentActive = false;
        handler.removeCallbacksAndMessages(null);
        if (mqttManager != null) {
            if (roleId != 3) {
                mqttManager.unsubscribe(MqttConfig.TOPIC_SENSOR_DATA1);
            }
            if (roleId != 2) {
                mqttManager.unsubscribe(MqttConfig.TOPIC_SENSOR_DATA2);
            }
        }
        binding = null;
    }
}