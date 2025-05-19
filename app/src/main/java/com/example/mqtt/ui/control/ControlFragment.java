package com.example.mqtt.ui.control;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mqtt.R;
import com.example.mqtt.MainActivity;
import com.example.mqtt.connectMQTT.MqttConfig;
import com.example.mqtt.connectMQTT.MqttManager;
import com.example.mqtt.model.Device;
import com.example.mqtt.model.DeviceResponse;
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

public class ControlFragment extends Fragment {
    private static final String TAG = "ControlFragment"; // Tag để ghi log
    private MqttManager mqttManager; // Quản lý kết nối MQTT
    private TabHost tabHost; // TabHost để hiển thị tab Trạm 1 và Trạm 2
    private boolean isFragmentActive = false; // Trạng thái hoạt động của fragment
    private int roleId; // Vai trò người dùng để phân quyền

    // Các thành phần giao diện cho Trạm 1
    private Switch modeSwitch1; // Công tắc chế độ tự động/thủ công
    private Button fanOnBtn1, fanOffBtn1, ledOnBtn1, ledOffBtn1; // Nút điều khiển quạt và đèn
    private TextView tvCO1, tvH21, tvNH31, fanStatus1, ledStatus1; // Hiển thị dữ liệu cảm biến và trạng thái

    // Các thành phần giao diện cho Trạm 2
    private Switch modeSwitch2;
    private Button fanOnBtn2, fanOffBtn2, ledOnBtn2, ledOffBtn2;
    private TextView tvCO2, tvH22, tvNH32, fanStatus2, ledStatus2;

    // Map lưu ngưỡng cảm biến
    private Map<String, Float> thresholdMap = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Gắn kết layout với fragment
        View view = inflater.inflate(R.layout.fragment_control, container, false);

        // Khởi tạo TabHost để quản lý các tab
        tabHost = view.findViewById(R.id.controlfan);
        tabHost.setup();

        // Lấy roleId từ SharedPreferences để phân quyền
        SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        roleId = prefs.getInt("role_id", 1); // Mặc định là 1 (Super Admin)
        Log.d(TAG, "Role ID nhận được: " + roleId);

        // Thêm tab dựa trên vai trò
        if (roleId == 1) {
            // Super Admin: hiển thị cả hai tab
            TabHost.TabSpec tab1 = tabHost.newTabSpec("Trạm 1");
            tab1.setContent(R.id.tabfan1);
            tab1.setIndicator("Trạm 1");
            tabHost.addTab(tab1);

            TabHost.TabSpec tab2 = tabHost.newTabSpec("Trạm 2");
            tab2.setContent(R.id.tabfan2);
            tab2.setIndicator("Trạm 2");
            tabHost.addTab(tab2);
        } else if (roleId == 2) {
            // Admin Trạm 1: chỉ hiển thị tab Trạm 1
            TabHost.TabSpec tab1 = tabHost.newTabSpec("Trạm 1");
            tab1.setContent(R.id.tabfan1);
            tab1.setIndicator("Trạm 1");
            tabHost.addTab(tab1);
            hideTabContent(view, R.id.tabfan2); // Ẩn nội dung Trạm 2
        } else if (roleId == 3) {
            // Admin Trạm 2: chỉ hiển thị tab Trạm 2
            TabHost.TabSpec tab2 = tabHost.newTabSpec("Trạm 2");
            tab2.setContent(R.id.tabfan2);
            tab2.setIndicator("Trạm 2");
            tabHost.addTab(tab2);
            hideTabContent(view, R.id.tabfan1); // Ẩn nội dung Trạm 1
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        isFragmentActive = true;

        // Lấy instance MqttManager từ MainActivity
        MainActivity mainActivity = (MainActivity) requireActivity();
        mqttManager = mainActivity.getMqttManager();

        // Khởi tạo giao diện cho Trạm 1
        View station1View = tabHost.getTabContentView().findViewById(R.id.tabfan1);
        initializeStation1Views(station1View);

        // Khởi tạo giao diện cho Trạm 2
        View station2View = tabHost.getTabContentView().findViewById(R.id.tabfan2);
        initializeStation2Views(station2View);

        // Thiết lập giao diện ban đầu
        setupInitialUI();

        // Lấy dữ liệu ngưỡng từ API
        fetchThresholdData();

        // Kiểm tra kết nối MQTT và khởi tạo nếu đã kết nối
        if (mainActivity.isMqttConnected()) {
            onMqttConnected();
        }
    }

    // Ẩn nội dung tab không được phép truy cập
    private void hideTabContent(View view, int tabContentId) {
        View tabContent = view.findViewById(tabContentId);
        if (tabContent != null) {
            tabContent.setVisibility(View.GONE);
        }
    }

    // Khởi tạo các thành phần giao diện cho Trạm 1
    private void initializeStation1Views(View station1View) {
        if (station1View != null) {
            modeSwitch1 = station1View.findViewById(R.id.mode_switch1);
            fanOnBtn1 = station1View.findViewById(R.id.fan_on_btn1);
            fanOffBtn1 = station1View.findViewById(R.id.fan_off_btn1);
            ledOnBtn1 = station1View.findViewById(R.id.led_on_btn1);
            ledOffBtn1 = station1View.findViewById(R.id.led_off_btn1);
            tvCO1 = station1View.findViewById(R.id.tvCO_1);
            tvH21 = station1View.findViewById(R.id.tvH2_1);
            tvNH31 = station1View.findViewById(R.id.tvNH3_1);
            fanStatus1 = station1View.findViewById(R.id.fan_status1);
            ledStatus1 = station1View.findViewById(R.id.led_status1);

            setupControlsForStation1(); // Thiết lập sự kiện điều khiển
        }
    }

    // Khởi tạo các thành phần giao diện cho Trạm 2
    private void initializeStation2Views(View station2View) {
        if (station2View != null) {
            modeSwitch2 = station2View.findViewById(R.id.mode_switch2);
            fanOnBtn2 = station2View.findViewById(R.id.fan_on_btn2);
            fanOffBtn2 = station2View.findViewById(R.id.fan_off_btn2);
            ledOnBtn2 = station2View.findViewById(R.id.led_on_btn2);
            ledOffBtn2 = station2View.findViewById(R.id.led_off_btn2);
            tvCO2 = station2View.findViewById(R.id.tvCO_22);
            tvH22 = station2View.findViewById(R.id.tvH2_22);
            tvNH32 = station2View.findViewById(R.id.tvNH3_22);
            fanStatus2 = station2View.findViewById(R.id.fan_status2);
            ledStatus2 = station2View.findViewById(R.id.led_status2);

            setupControlsForStation2(); // Thiết lập sự kiện điều khiển
        }
    }

    // Thiết lập giao diện ban đầu
    private void setupInitialUI() {
        // Trạm 1
        if (roleId != 3) {
            tvCO1.setText("--");
            tvH21.setText("--");
            tvNH31.setText("--");
            fanStatus1.setText("Trạng thái: Đang kết nối...");
            ledStatus1.setText("Trạng thái: Đang kết nối...");
            setControlsEnabledForStation1(false); // Vô hiệu hóa điều khiển
        }

        // Trạm 2
        if (roleId != 2) {
            tvCO2.setText("--");
            tvH22.setText("--");
            tvNH32.setText("--");
            fanStatus2.setText("Trạng thái: Đang kết nối...");
            ledStatus2.setText("Trạng thái: Đang kết nối...");
            setControlsEnabledForStation2(false); // Vô hiệu hóa điều khiển
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
                        Log.d(TAG, "Phản hồi API: " + responseData);
                        DeviceResponse deviceResponse = new Gson().fromJson(responseData, DeviceResponse.class);
                        if (deviceResponse.isSuccess() && deviceResponse.getDevices() != null) {
                            for (Device device : deviceResponse.getDevices()) {
                                if (device.getThreshold() != null) {
                                    thresholdMap.put(device.getSensorId(), device.getThreshold());
                                    Log.d(TAG, "Ngưỡng được tải: " + device.getSensorId() + " = " + device.getThreshold());
                                }
                            }
                            if (thresholdMap.isEmpty()) {
                                Log.w(TAG, "Không tải được ngưỡng từ API");
                                showToast("Không tải được ngưỡng từ API");
                            }
                        } else {
                            Log.w(TAG, "Phản hồi API không thành công hoặc không có thiết bị");
                            showToast("Dữ liệu ngưỡng không hợp lệ");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Lỗi phân tích dữ liệu ngưỡng", e);
                        showToast("Lỗi khi xử lý dữ liệu ngưỡng");
                    }
                } else {
                    Log.w(TAG, "Gọi API thất bại: " + response.code());
                    showToast("Không thể lấy dữ liệu ngưỡng");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "Không thể lấy dữ liệu ngưỡng", t);
                showToast("Lỗi kết nối khi lấy dữ liệu ngưỡng");
            }
        });
    }

    // Xử lý khi kết nối MQTT thành công
    public void onMqttConnected() {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> {
            if (!isAdded()) return;

            if (roleId != 3) {
                setControlsEnabledForStation1(true); // Kích hoạt điều khiển Trạm 1
                subscribeToMqttTopicsForStation1(); // Đăng ký các topic MQTT
            }
            if (roleId != 2) {
                setControlsEnabledForStation2(true); // Kích hoạt điều khiển Trạm 2
                subscribeToMqttTopicsForStation2(); // Đăng ký các topic MQTT
            }

            if (roleId != 3) {
                requestCurrentStatusForStation1(); // Yêu cầu trạng thái hiện tại
            }
            if (roleId != 2) {
                requestCurrentStatusForStation2(); // Yêu cầu trạng thái hiện tại
            }
        });
    }

    // Yêu cầu trạng thái hiện tại của Trạm 1
    private void requestCurrentStatusForStation1() {
        if (mqttManager != null && mqttManager.isConnected()) {
            mqttManager.publish(MqttConfig.TOPIC_REQUEST_STATUS_1, "GET_STATUS");
            Log.d(TAG, "Đã yêu cầu trạng thái cho Trạm 1");
        } else {
            Log.w(TAG, "MQTT không kết nối, không thể yêu cầu trạng thái cho Trạm 1");
        }
    }

    // Yêu cầu trạng thái hiện tại của Trạm 2
    private void requestCurrentStatusForStation2() {
        if (mqttManager != null && mqttManager.isConnected()) {
            mqttManager.publish(MqttConfig.TOPIC_REQUEST_STATUS_2, "GET_STATUS");
            Log.d(TAG, "Đã yêu cầu trạng thái cho Trạm 2");
        } else {
            Log.w(TAG, "MQTT không kết nối, không thể yêu cầu trạng thái cho Trạm 2");
        }
    }

    // Thiết lập sự kiện điều khiển cho Trạm 1
    private void setupControlsForStation1() {
        if (modeSwitch1 != null) {
            modeSwitch1.setOnCheckedChangeListener((buttonView, isChecked) -> {
                String mode = isChecked ? "AUTO" : "MANUAL";
                if (mqttManager != null && mqttManager.isConnected()) {
                    mqttManager.publish(MqttConfig.TOPIC_MODE_CONTROL1, mode); // Gửi chế độ qua MQTT
                    Log.d(TAG, "Đã gửi chế độ " + mode + " cho Trạm 1");
                } else {
                    Log.w(TAG, "MQTT không kết nối, chế độ không được gửi cho Trạm 1");
                }
                setManualControlsEnabledForStation1(!isChecked); // Cập nhật trạng thái điều khiển thủ công
                Toast.makeText(getContext(), "Trạm 1 - Chế độ: " + (isChecked ? "Tự động" : "Bằng tay"), Toast.LENGTH_SHORT).show();
            });
        }

        // Thiết lập sự kiện cho các nút điều khiển
        if (fanOnBtn1 != null) fanOnBtn1.setOnClickListener(v -> controlDevice(MqttConfig.TOPIC_FAN_CONTROL1, "ON", fanStatus1, "quạt"));
        if (fanOffBtn1 != null) fanOffBtn1.setOnClickListener(v -> controlDevice(MqttConfig.TOPIC_FAN_CONTROL1, "OFF", fanStatus1, "quạt"));
        if (ledOnBtn1 != null) ledOnBtn1.setOnClickListener(v -> controlDevice(MqttConfig.TOPIC_LED_CONTROL1, "ON", ledStatus1, "đèn"));
        if (ledOffBtn1 != null) ledOffBtn1.setOnClickListener(v -> controlDevice(MqttConfig.TOPIC_LED_CONTROL1, "OFF", ledStatus1, "đèn"));
    }

    // Thiết lập sự kiện điều khiển cho Trạm 2
    private void setupControlsForStation2() {
        if (modeSwitch2 != null) {
            modeSwitch2.setOnCheckedChangeListener((buttonView, isChecked) -> {
                String mode = isChecked ? "AUTO" : "MANUAL";
                if (mqttManager != null && mqttManager.isConnected()) {
                    mqttManager.publish(MqttConfig.TOPIC_MODE_CONTROL2, mode); // Gửi chế độ qua MQTT
                    Log.d(TAG, "Đã gửi chế độ " + mode + " cho Trạm 2");
                } else {
                    Log.w(TAG, "MQTT không kết nối, chế độ không được gửi cho Trạm 2");
                }
                setManualControlsEnabledForStation2(!isChecked); // Cập nhật trạng thái điều khiển thủ công
                Toast.makeText(getContext(), "Trạm 2 - Chế độ: " + (isChecked ? "Tự động" : "Bằng tay"), Toast.LENGTH_SHORT).show();
            });
        }

        // Thiết lập sự kiện cho các nút điều khiển
        if (fanOnBtn2 != null) fanOnBtn2.setOnClickListener(v -> controlDevice(MqttConfig.TOPIC_FAN_CONTROL2, "ON", fanStatus2, "quạt"));
        if (fanOffBtn2 != null) fanOffBtn2.setOnClickListener(v -> controlDevice(MqttConfig.TOPIC_FAN_CONTROL2, "OFF", fanStatus2, "quạt"));
        if (ledOnBtn2 != null) ledOnBtn2.setOnClickListener(v -> controlDevice(MqttConfig.TOPIC_LED_CONTROL2, "ON", ledStatus2, "đèn"));
        if (ledOffBtn2 != null) ledOffBtn2.setOnClickListener(v -> controlDevice(MqttConfig.TOPIC_LED_CONTROL2, "OFF", ledStatus2, "đèn"));
    }

    // Điều khiển thiết bị qua MQTT
    private void controlDevice(String topic, String command, TextView statusView, String deviceName) {
        if (mqttManager != null && mqttManager.isConnected()) {
            mqttManager.publish(topic, command); // Gửi lệnh qua MQTT
            statusView.setText("Trạng thái: " + (command.equals("ON") ? "Đang bật" : "Đang tắt"));
            Log.d(TAG, "Đã gửi " + command + " đến " + topic);
            Toast.makeText(getContext(), "Đã " + (command.equals("ON") ? "bật" : "tắt") + " " + deviceName, Toast.LENGTH_SHORT).show();
        } else {
            Log.w(TAG, "MQTT không kết nối, không thể điều khiển " + deviceName);
            showToast("Không thể điều khiển, MQTT không kết nối");
        }
    }

    // Đăng ký các topic MQTT cho Trạm 1
    private void subscribeToMqttTopicsForStation1() {
        if (roleId != 3 && mqttManager != null && mqttManager.isConnected()) {
            mqttManager.subscribe(MqttConfig.TOPIC_SENSOR_DATA1, 1, message -> {
                try {
                    JSONObject json = new JSONObject(message);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (!isAdded() || getActivity() == null) return;

                            try {
                                if (json.has("Lab001CO")) {
                                    int co = json.getInt("Lab001CO");
                                    int h2 = json.getInt("Lab001H2");
                                    int nh3 = json.getInt("Lab001NH3");

                                    tvCO1.setText(String.valueOf(co));
                                    tvH21.setText(String.valueOf(h2));
                                    tvNH31.setText(String.valueOf(nh3));
                                    Log.d(TAG, "Nhận dữ liệu cảm biến cho Trạm 1: CO=" + co + ", H2=" + h2 + ", NH3=" + nh3);

                                    if (modeSwitch1 != null && modeSwitch1.isChecked() && !thresholdMap.isEmpty()) {
                                        checkThresholdForStation1(co, h2, nh3); // Kiểm tra ngưỡng
                                    } else {
                                        Log.d(TAG, "Chế độ tự động không kích hoạt hoặc không có ngưỡng cho Trạm 1");
                                    }
                                }
                            } catch (JSONException e) {
                                Log.e(TAG, "Lỗi phân tích dữ liệu Trạm 1", e);
                                showToast("Lỗi dữ liệu cảm biến Trạm 1");
                            }
                        });
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Lỗi JSON Trạm 1", e);
                    showToast("Lỗi phân tích dữ liệu Trạm 1");
                }
            });

            mqttManager.subscribe(MqttConfig.TOPIC_FAN_STATUS1, 1, message -> {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (!isAdded()) return;
                        fanStatus1.setText("Trạng thái: " + message);
                        updateFanButtonStatesForStation1(message); // Cập nhật trạng thái nút
                        Log.d(TAG, "Cập nhật trạng thái quạt cho Trạm 1: " + message);
                    });
                }
            });

            mqttManager.subscribe(MqttConfig.TOPIC_LED_STATUS1, 1, message -> {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (!isAdded()) return;
                        ledStatus1.setText("Trạng thái: " + message);
                        updateLedButtonStatesForStation1(message); // Cập nhật trạng thái nút
                        Log.d(TAG, "Cập nhật trạng thái đèn cho Trạm 1: " + message);
                    });
                }
            });

            mqttManager.subscribe(MqttConfig.TOPIC_MODE_STATUS1, 1, message -> {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (!isAdded()) return;
                        boolean isAuto = message.equals("AUTO");
                        modeSwitch1.setChecked(isAuto);
                        setManualControlsEnabledForStation1(!isAuto); // Cập nhật trạng thái điều khiển
                        Log.d(TAG, "Cập nhật trạng thái chế độ cho Trạm 1: " + message);
                    });
                }
            });
        } else {
            Log.w(TAG, "Trạm 1 không được đăng ký, roleId=" + roleId + " hoặc MQTT không kết nối");
        }
    }

    // Đăng ký các topic MQTT cho Trạm 2
    private void subscribeToMqttTopicsForStation2() {
        if (roleId != 2 && mqttManager != null && mqttManager.isConnected()) {
            mqttManager.subscribe(MqttConfig.TOPIC_SENSOR_DATA2, 1, message -> {
                try {
                    JSONObject json = new JSONObject(message);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (!isAdded() || getActivity() == null) return;

                            try {
                                if (json.has("A4002CO")) {
                                    int co = json.getInt("A4002CO");
                                    int h2 = json.getInt("A4002H2");
                                    int nh3 = json.getInt("A4002NH3");

                                    tvCO2.setText(String.valueOf(co));
                                    tvH22.setText(String.valueOf(h2));
                                    tvNH32.setText(String.valueOf(nh3));
                                    Log.d(TAG, "Nhận dữ liệu cảm biến cho Trạm 2: CO=" + co + ", H2=" + h2 + ", NH3=" + nh3);

                                    if (modeSwitch2 != null && modeSwitch2.isChecked() && !thresholdMap.isEmpty()) {
                                        checkThresholdForStation2(co, h2, nh3); // Kiểm tra ngưỡng
                                    } else {
                                        Log.d(TAG, "Chế độ tự động không kích hoạt hoặc không có ngưỡng cho Trạm 2");
                                    }
                                }
                            } catch (JSONException e) {
                                Log.e(TAG, "Lỗi phân tích dữ liệu Trạm 2", e);
                                showToast("Lỗi dữ liệu cảm biến Trạm 2");
                            }
                        });
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Lỗi JSON Trạm 2", e);
                    showToast("Lỗi phân tích dữ liệu Trạm 2");
                }
            });

            mqttManager.subscribe(MqttConfig.TOPIC_FAN_STATUS2, 1, message -> {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (!isAdded()) return;
                        fanStatus2.setText("Trạng thái: " + message);
                        updateFanButtonStatesForStation2(message); // Cập nhật trạng thái nút
                        Log.d(TAG, "Cập nhật trạng thái quạt cho Trạm 2: " + message);
                    });
                }
            });

            mqttManager.subscribe(MqttConfig.TOPIC_LED_STATUS2, 1, message -> {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (!isAdded()) return;
                        ledStatus2.setText("Trạng thái: " + message);
                        updateLedButtonStatesForStation2(message); // Cập nhật trạng thái nút
                        Log.d(TAG, "Cập nhật trạng thái đèn cho Trạm 2: " + message);
                    });
                }
            });

            mqttManager.subscribe(MqttConfig.TOPIC_MODE_STATUS2, 1, message -> {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (!isAdded()) return;
                        boolean isAuto = message.equals("AUTO");
                        modeSwitch2.setChecked(isAuto);
                        setManualControlsEnabledForStation2(!isAuto); // Cập nhật trạng thái điều khiển
                        Log.d(TAG, "Cập nhật trạng thái chế độ cho Trạm 2: " + message);
                    });
                }
            });
        } else {
            Log.w(TAG, "Trạm 2 không được đăng ký, roleId=" + roleId + " hoặc MQTT không kết nối");
        }
    }

    // Kiểm tra ngưỡng cho Trạm 1
    private void checkThresholdForStation1(int co, int h2, int nh3) {
        if (thresholdMap.isEmpty()) {
            Log.w(TAG, "Không có ngưỡng cho Trạm 1");
            return;
        }

        boolean thresholdExceeded = false;
        Float coThreshold = thresholdMap.get("Lab001CO");
        Float h2Threshold = thresholdMap.get("Lab001H2");
        Float nh3Threshold = thresholdMap.get("Lab001NH3");

        if (coThreshold != null && co > coThreshold) {
            thresholdExceeded = true;
            Log.d(TAG, "Trạm 1: CO vượt ngưỡng (" + co + " > " + coThreshold + ")");
        }
        if (h2Threshold != null && h2 > h2Threshold) {
            thresholdExceeded = true;
            Log.d(TAG, "Trạm 1: H2 vượt ngưỡng (" + h2 + " > " + h2Threshold + ")");
        }
        if (nh3Threshold != null && nh3 > nh3Threshold) {
            thresholdExceeded = true;
            Log.d(TAG, "Trạm 1: NH3 vượt ngưỡng (" + nh3 + " > " + nh3Threshold + ")");
        }

        if (thresholdExceeded) {
            if (fanStatus1 != null && !fanStatus1.getText().toString().contains("Đang bật")) {
                controlDevice(MqttConfig.TOPIC_FAN_CONTROL1, "ON", fanStatus1, "quạt");
            }
            if (ledStatus1 != null && !ledStatus1.getText().toString().contains("Đang bật")) {
                controlDevice(MqttConfig.TOPIC_LED_CONTROL1, "ON", ledStatus1, "đèn");
            }
            showToast("Trạm 1: Bật quạt và LED do vượt ngưỡng");
        }
    }

    // Kiểm tra ngưỡng cho Trạm 2
    private void checkThresholdForStation2(int co, int h2, int nh3) {
        if (thresholdMap.isEmpty()) {
            Log.w(TAG, "Không có ngưỡng cho Trạm 2");
            return;
        }

        boolean thresholdExceeded = false;
        Float coThreshold = thresholdMap.get("A4002CO");
        Float h2Threshold = thresholdMap.get("A4002H2");
        Float nh3Threshold = thresholdMap.get("A4002NH3");

        if (coThreshold != null && co > coThreshold) {
            thresholdExceeded = true;
            Log.d(TAG, "Trạm 2: CO vượt ngưỡng (" + co + " > " + coThreshold + ")");
        }
        if (h2Threshold != null && h2 > h2Threshold) {
            thresholdExceeded = true;
            Log.d(TAG, "Trạm 2: H2 vượt ngưỡng (" + h2 + " > " + h2Threshold + ")");
        }
        if (nh3Threshold != null && nh3 > nh3Threshold) {
            thresholdExceeded = true;
            Log.d(TAG, "Trạm 2: NH3 vượt ngưỡng (" + nh3 + " > " + nh3Threshold + ")");
        }

        if (thresholdExceeded) {
            if (fanStatus2 != null && !fanStatus2.getText().toString().contains("Đang bật")) {
                controlDevice(MqttConfig.TOPIC_FAN_CONTROL2, "ON", fanStatus2, "quạt");
            }
            if (ledStatus2 != null && !ledStatus2.getText().toString().contains("Đang bật")) {
                controlDevice(MqttConfig.TOPIC_LED_CONTROL2, "ON", ledStatus2, "đèn");
            }
            showToast("Trạm 2: Bật quạt và LED do vượt ngưỡng");
        }
    }

    // Cập nhật trạng thái nút quạt Trạm 1
    private void updateFanButtonStatesForStation1(String status) {
        if (fanOnBtn1 != null && fanOffBtn1 != null) {
            boolean isOn = status.equals("ON");
            fanOnBtn1.setEnabled(!isOn);
            fanOffBtn1.setEnabled(isOn);
        }
    }

    // Cập nhật trạng thái nút đèn Trạm 1
    private void updateLedButtonStatesForStation1(String status) {
        if (ledOnBtn1 != null && ledOffBtn1 != null) {
            boolean isOn = status.equals("ON");
            ledOnBtn1.setEnabled(!isOn);
            ledOffBtn1.setEnabled(isOn);
        }
    }

    // Kích hoạt/vô hiệu hóa điều khiển thủ công Trạm 1
    private void setManualControlsEnabledForStation1(boolean enabled) {
        if (fanOnBtn1 != null) fanOnBtn1.setEnabled(enabled);
        if (fanOffBtn1 != null) fanOffBtn1.setEnabled(enabled);
        if (ledOnBtn1 != null) ledOnBtn1.setEnabled(enabled);
        if (ledOffBtn1 != null) ledOffBtn1.setEnabled(enabled);
    }

    // Kích hoạt/vô hiệu hóa toàn bộ điều khiển Trạm 1
    private void setControlsEnabledForStation1(boolean enabled) {
        if (modeSwitch1 != null) modeSwitch1.setEnabled(enabled);
        setManualControlsEnabledForStation1(enabled && modeSwitch1 != null && !modeSwitch1.isChecked());
    }

    // Cập nhật trạng thái nút quạt Trạm 2
    private void updateFanButtonStatesForStation2(String status) {
        if (fanOnBtn2 != null && fanOffBtn2 != null) {
            boolean isOn = status.equals("ON");
            fanOnBtn2.setEnabled(!isOn);
            fanOffBtn2.setEnabled(isOn);
        }
    }

    // Cập nhật trạng thái nút đèn Trạm 2
    private void updateLedButtonStatesForStation2(String status) {
        if (ledOnBtn2 != null && ledOffBtn2 != null) {
            boolean isOn = status.equals("ON");
            ledOnBtn2.setEnabled(!isOn);
            ledOffBtn2.setEnabled(isOn);
        }
    }

    // Kích hoạt/vô hiệu hóa điều khiển thủ công Trạm 2
    private void setManualControlsEnabledForStation2(boolean enabled) {
        if (fanOnBtn2 != null) fanOnBtn2.setEnabled(enabled);
        if (fanOffBtn2 != null) fanOffBtn2.setEnabled(enabled);
        if (ledOnBtn2 != null) ledOnBtn2.setEnabled(enabled);
        if (ledOffBtn2 != null) ledOffBtn2.setEnabled(enabled);
    }

    // Kích hoạt/vô hiệu hóa toàn bộ điều khiển Trạm 2
    private void setControlsEnabledForStation2(boolean enabled) {
        if (modeSwitch2 != null) modeSwitch2.setEnabled(enabled);
        setManualControlsEnabledForStation2(enabled && modeSwitch2 != null && !modeSwitch2.isChecked());
    }

    // Hiển thị thông báo
    private void showToast(String message) {
        if (isAdded() && getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isFragmentActive = false;

        // Hủy đăng ký các topic MQTT khi fragment bị hủy
        if (mqttManager != null) {
            if (roleId != 3) {
                mqttManager.unsubscribe(MqttConfig.TOPIC_SENSOR_DATA1);
                mqttManager.unsubscribe(MqttConfig.TOPIC_FAN_STATUS1);
                mqttManager.unsubscribe(MqttConfig.TOPIC_LED_STATUS1);
                mqttManager.unsubscribe(MqttConfig.TOPIC_MODE_STATUS1);
            }
            if (roleId != 2) {
                mqttManager.unsubscribe(MqttConfig.TOPIC_SENSOR_DATA2);
                mqttManager.unsubscribe(MqttConfig.TOPIC_FAN_STATUS2);
                mqttManager.unsubscribe(MqttConfig.TOPIC_LED_STATUS2);
                mqttManager.unsubscribe(MqttConfig.TOPIC_MODE_STATUS2);
            }
        }
    }
}