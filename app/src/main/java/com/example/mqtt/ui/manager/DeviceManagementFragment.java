package com.example.mqtt.ui.manager;

import android.app.AlertDialog;
import android.content.Context;
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
import com.example.mqtt.model.Device;
import com.example.mqtt.network.ApiClient;
import com.example.mqtt.network.ApiService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DeviceManagementFragment extends Fragment {
    private static final String TAG = "DeviceManagement"; // Tag để ghi log
    private ListView listViewDevices; // ListView hiển thị danh sách thiết bị
    private Button btnBack, btnAdd; // Nút quay lại và thêm thiết bị
    private DeviceAdapter adapter; // Adapter cho ListView
    private List<Device> deviceList; // Danh sách thiết bị
    private ApiService apiService; // Dịch vụ API để gọi các endpoint

    // Handler để tự động làm mới danh sách thiết bị
    private Handler refreshHandler = new Handler();
    private static final long REFRESH_INTERVAL = 30000; // Làm mới mỗi 30 giây
    private boolean isRefreshing = false; // Trạng thái làm mới

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Khởi tạo ApiService từ ApiClient
        apiService = ApiClient.getClient().create(ApiService.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Gắn kết layout với fragment
        View view = inflater.inflate(R.layout.fragment_device_management, container, false);

        // Khởi tạo các thành phần giao diện
        listViewDevices = view.findViewById(R.id.listViewDevices);
        btnBack = view.findViewById(R.id.btnBack);
        btnAdd = view.findViewById(R.id.btnAdd);

        // Khởi tạo danh sách thiết bị và adapter
        deviceList = new ArrayList<>();
        adapter = new DeviceAdapter(requireContext(), deviceList);
        listViewDevices.setAdapter(adapter);

        // Xử lý sự kiện nút quay lại
        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());
        // Xử lý sự kiện nút thêm thiết bị
        btnAdd.setOnClickListener(v -> showAddDeviceDialog());

        // Tải danh sách thiết bị khi khởi tạo
        loadDevices();
        // Bắt đầu tự động làm mới
        startAutoRefresh();

        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        // Dừng tự động làm mới khi fragment không hoạt động
        stopAutoRefresh();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Tải lại danh sách thiết bị và tiếp tục tự động làm mới khi fragment hoạt động trở lại
        loadDevices();
        startAutoRefresh();
    }

    // Bắt đầu cơ chế tự động làm mới danh sách thiết bị
    private void startAutoRefresh() {
        if (!isRefreshing) {
            isRefreshing = true;
            refreshHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    loadDevices(); // Tải lại danh sách
                    refreshHandler.postDelayed(this, REFRESH_INTERVAL); // Lặp lại sau 30 giây
                }
            }, REFRESH_INTERVAL);
        }
    }

    // Dừng cơ chế tự động làm mới
    private void stopAutoRefresh() {
        isRefreshing = false;
        refreshHandler.removeCallbacksAndMessages(null);
    }

    // Tải danh sách thiết bị từ API
    public void loadDevices() {
        Log.d(TAG, "Đang tải danh sách thiết bị...");
        apiService.getDevices().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String jsonString = response.body().string();
                        Log.d(TAG, "Phản hồi: " + jsonString);
                        JSONArray dataArray = new JSONArray(jsonString);
                        deviceList.clear(); // Xóa danh sách cũ
                        // Phân tích dữ liệu JSON và thêm vào danh sách
                        for (int i = 0; i < dataArray.length(); i++) {
                            JSONObject deviceObj = dataArray.getJSONObject(i);
                            Device device = new Device();
                            device.setSensorId(deviceObj.getString("sensor_id"));
                            device.setNameSensor(deviceObj.getString("name_sensor"));
                            device.setLocation(deviceObj.getString("location"));
                            device.setType(deviceObj.getString("type"));
                            device.setCreatedAt(deviceObj.getString("created_at"));
                            device.setDonvi(deviceObj.getString("donvi"));
                            if (!deviceObj.isNull("threshold")) {
                                device.setThreshold((float) deviceObj.getDouble("threshold"));
                            }
                            deviceList.add(device);
                        }
                        if (isAdded()) { // Kiểm tra fragment còn gắn với activity không
                            adapter.notifyDataSetChanged(); // Cập nhật giao diện
                            Log.d(TAG, "Đã tải " + deviceList.size() + " thiết bị");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Lỗi xử lý dữ liệu: " + e.getMessage(), e);
                        showError("Lỗi xử lý dữ liệu: " + e.getMessage());
                    }
                } else {
                    try {
                        String errorBody = response.errorBody() != null ?
                                response.errorBody().string() : "Lỗi không xác định";
                        Log.e(TAG, "Không thể tải thiết bị. Mã lỗi: " + response.code()
                                + ", Lỗi: " + errorBody);
                        showError("Không thể tải thiết bị. Mã lỗi: " + response.code());
                    } catch (IOException e) {
                        Log.e(TAG, "Lỗi đọc nội dung lỗi", e);
                        showError("Không thể tải thiết bị. Mã lỗi: " + response.code());
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "Lỗi kết nối: " + t.getMessage(), t);
                showError("Lỗi kết nối: " + t.getMessage());
            }
        });
    }

    // Hiển thị dialog để thêm thiết bị mới
    private void showAddDeviceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Thêm thiết bị mới");

        // Gắn layout dialog
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_device, null);
        builder.setView(dialogView);

        // Khởi tạo các trường nhập liệu
        EditText etSensorId = dialogView.findViewById(R.id.etSensorId);
        EditText etNameSensor = dialogView.findViewById(R.id.etNameSensor);
        EditText etLocation = dialogView.findViewById(R.id.etLocation);
        EditText etType = dialogView.findViewById(R.id.etType);
        EditText etDonvi = dialogView.findViewById(R.id.etDonvi);
        EditText etThreshold = dialogView.findViewById(R.id.etThreshold);

        builder.setPositiveButton("Thêm", (dialog, which) -> {
            // Lấy dữ liệu từ các trường nhập
            String sensorId = etSensorId.getText().toString().trim();
            String nameSensor = etNameSensor.getText().toString().trim();
            String location = etLocation.getText().toString().trim();
            String type = etType.getText().toString().trim();
            String donvi = etDonvi.getText().toString().trim();
            String thresholdStr = etThreshold.getText().toString().trim();

            // Kiểm tra dữ liệu bắt buộc
            if (sensorId.isEmpty() || nameSensor.isEmpty() || location.isEmpty() || type.isEmpty() || donvi.isEmpty()) {
                showError("Vui lòng điền đầy đủ thông tin");
                return;
            }

            try {
                // Tạo đối tượng thiết bị mới
                Device device = new Device();
                device.setSensorId(sensorId);
                device.setNameSensor(nameSensor);
                device.setLocation(location);
                device.setType(type);
                device.setDonvi(donvi);

                if (!thresholdStr.isEmpty()) {
                    device.setThreshold(Float.parseFloat(thresholdStr));
                }

                // Gọi API để thêm thiết bị
                apiService.addDevice(device).enqueue(new DeviceOperationCallback("Thêm thiết bị thành công"));
            } catch (NumberFormatException e) {
                showError("Ngưỡng phải là số");
            } catch (Exception e) {
                Log.e(TAG, "Lỗi tạo dữ liệu: " + e.getMessage(), e);
                showError("Lỗi tạo dữ liệu: " + e.getMessage());
            }
        });

        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    // Hiển thị dialog để sửa thông tin thiết bị
    private void showEditDeviceDialog(Device device) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Sửa thiết bị");

        // Gắn layout dialog
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_device, null);
        builder.setView(dialogView);

        // Khởi tạo các trường nhập liệu và điền dữ liệu hiện tại
        EditText etSensorId = dialogView.findViewById(R.id.etSensorId);
        EditText etNameSensor = dialogView.findViewById(R.id.etNameSensor);
        EditText etLocation = dialogView.findViewById(R.id.etLocation);
        EditText etType = dialogView.findViewById(R.id.etType);
        EditText etDonvi = dialogView.findViewById(R.id.etDonvi);
        EditText etThreshold = dialogView.findViewById(R.id.etThreshold);

        etSensorId.setText(device.getSensorId());
        etSensorId.setEnabled(false); // Không cho sửa sensorId
        etNameSensor.setText(device.getNameSensor());
        etLocation.setText(device.getLocation());
        etType.setText(device.getType());
        etDonvi.setText(device.getDonvi());
        if (device.getThreshold() != null) {
            etThreshold.setText(String.valueOf(device.getThreshold()));
        }

        builder.setPositiveButton("Lưu", (dialog, which) -> {
            // Lấy dữ liệu từ các trường nhập
            String nameSensor = etNameSensor.getText().toString().trim();
            String location = etLocation.getText().toString().trim();
            String type = etType.getText().toString().trim();
            String donvi = etDonvi.getText().toString().trim();
            String thresholdStr = etThreshold.getText().toString().trim();

            // Kiểm tra dữ liệu bắt buộc
            if (nameSensor.isEmpty() || location.isEmpty() || type.isEmpty() || donvi.isEmpty()) {
                showError("Vui lòng điền đầy đủ thông tin");
                return;
            }

            try {
                // Tạo đối tượng thiết bị cập nhật
                Device updatedDevice = new Device();
                updatedDevice.setSensorId(device.getSensorId());
                updatedDevice.setNameSensor(nameSensor);
                updatedDevice.setLocation(location);
                updatedDevice.setType(type);
                updatedDevice.setDonvi(donvi);

                if (!thresholdStr.isEmpty()) {
                    updatedDevice.setThreshold(Float.parseFloat(thresholdStr));
                }

                Log.d(TAG, "Cập nhật thiết bị: " + updatedDevice.toString());

                // Gọi API để cập nhật thiết bị
                apiService.updateDevice(updatedDevice.getSensorId(), updatedDevice)
                        .enqueue(new Callback<ResponseBody>() {
                            @Override
                            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                try {
                                    if (response.isSuccessful()) {
                                        String responseBody = response.body() != null ?
                                                response.body().string() : "{}";
                                        Log.d(TAG, "Phản hồi cập nhật thiết bị: " + responseBody);
                                        showSuccess("Cập nhật thiết bị thành công");
                                        loadDevices(); // Tải lại danh sách
                                    } else {
                                        String errorBody = response.errorBody() != null ?
                                                response.errorBody().string() : "Lỗi không xác định";
                                        Log.e(TAG, "Lỗi cập nhật thiết bị: " + errorBody);
                                        showError("Lỗi: " + errorBody);
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Lỗi xử lý phản hồi", e);
                                    showError("Lỗi xử lý phản hồi");
                                }
                            }

                            @Override
                            public void onFailure(Call<ResponseBody> call, Throwable t) {
                                Log.e(TAG, "Lỗi kết nối", t);
                                showError("Lỗi kết nối: " + t.getMessage());
                            }
                        });
            } catch (NumberFormatException e) {
                showError("Ngưỡng phải là số");
            }
        });

        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    // Xóa thiết bị
    private void deleteDevice(Device device) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa thiết bị " + device.getNameSensor() + "?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    // Gọi API để xóa thiết bị
                    apiService.deleteDevice(device.getSensorId()).enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            try {
                                if (response.isSuccessful()) {
                                    showSuccess("Xóa thiết bị thành công");
                                    loadDevices(); // Tải lại danh sách
                                } else {
                                    String errorBody = response.errorBody() != null ?
                                            response.errorBody().string() : "Lỗi không xác định";
                                    Log.e(TAG, "Lỗi xóa thiết bị: " + response.code() + ", " + errorBody);
                                    loadDevices(); // Vẫn tải lại để đồng bộ
                                }
                            } catch (IOException e) {
                                Log.e(TAG, "Lỗi đọc phản hồi", e);
                                showError("Lỗi đọc phản hồi: " + e.getMessage());
                                loadDevices(); // Vẫn tải lại để đồng bộ
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            Log.e(TAG, "Lỗi kết nối khi xóa thiết bị", t);
                            showError("Lỗi kết nối: " + t.getMessage());
                            loadDevices(); // Vẫn tải lại để đồng bộ
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    // Callback xử lý phản hồi từ API (thêm, cập nhật, xóa)
    private class DeviceOperationCallback implements Callback<ResponseBody> {
        private final String successMessage;

        public DeviceOperationCallback(String successMessage) {
            this.successMessage = successMessage;
        }

        @Override
        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
            try {
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        String responseBody = response.body().string();
                        Log.d(TAG, "Phản hồi API: " + responseBody);

                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            showSuccess(successMessage); // Hiển thị thông báo thành công
                        } catch (JSONException e) {
                            Log.d(TAG, "Phản hồi không phải JSON hoặc định dạng bất ngờ: " + responseBody);
                            showSuccess(successMessage); // Giả định thành công nếu mã 200 OK
                        }
                    } else {
                        showSuccess(successMessage); // Phản hồi thành công nhưng body rỗng
                    }
                    loadDevices(); // Tải lại danh sách
                } else {
                    String errorBody = response.errorBody() != null ?
                            response.errorBody().string() : "Lỗi không xác định";
                    Log.e(TAG, "Phản hồi lỗi: " + response.code() + ", " + errorBody);
                    showError("Thao tác thất bại: " + response.code());
                    loadDevices(); // Vẫn tải lại để đồng bộ
                }
            } catch (Exception e) {
                Log.e(TAG, "Lỗi xử lý phản hồi", e);
                showError("Lỗi xử lý phản hồi: " + e.getMessage());
                loadDevices(); // Vẫn tải lại để đồng bộ
            }
        }

        @Override
        public void onFailure(Call<ResponseBody> call, Throwable t) {
            Log.e(TAG, "Yêu cầu thất bại", t);
            showError("Lỗi kết nối: " + t.getMessage());
        }
    }

    // Hiển thị thông báo thành công
    private void showSuccess(String message) {
        if (isAdded()) { // Kiểm tra fragment còn gắn với activity không
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            });
        }
    }

    // Hiển thị thông báo lỗi
    private void showError(String message) {
        if (isAdded()) { // Kiểm tra fragment còn gắn với activity không
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            });
        }
    }

    // Adapter để hiển thị danh sách thiết bị trong ListView
    private class DeviceAdapter extends ArrayAdapter<Device> {
        public DeviceAdapter(Context context, List<Device> devices) {
            super(context, 0, devices);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            Device device = getItem(position);
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_device, parent, false);
            }

            // Khởi tạo các thành phần giao diện trong item
            TextView tvSensorName = convertView.findViewById(R.id.tvSensorName);
            TextView tvSensorId = convertView.findViewById(R.id.tvSensorId);
            TextView tvLocation = convertView.findViewById(R.id.tvLocation);
            TextView tvType = convertView.findViewById(R.id.tvType);
            TextView tvDonvi = convertView.findViewById(R.id.tvDonvi);
            TextView tvThreshold = convertView.findViewById(R.id.tvThreshold);
            Button btnEdit = convertView.findViewById(R.id.btnEdit);
            Button btnDelete = convertView.findViewById(R.id.btnDelete);

            if (device != null) {
                // Cập nhật thông tin thiết bị lên giao diện
                tvSensorName.setText(device.getNameSensor());
                tvSensorId.setText("Mã: " + device.getSensorId());
                tvLocation.setText("Vị trí: " + device.getLocation());
                tvType.setText("Loại: " + device.getType());
                tvDonvi.setText("Đơn vị: " + device.getDonvi());

                if (device.getThreshold() != null) {
                    tvThreshold.setText("Ngưỡng: " + device.getThreshold());
                    tvThreshold.setVisibility(View.VISIBLE);
                } else {
                    tvThreshold.setVisibility(View.GONE);
                }

                // Xử lý sự kiện nút sửa và xóa
                btnEdit.setOnClickListener(v -> showEditDeviceDialog(device));
                btnDelete.setOnClickListener(v -> deleteDevice(device));
            }

            return convertView;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Dừng tự động làm mới khi fragment bị hủy
        stopAutoRefresh();
    }
}