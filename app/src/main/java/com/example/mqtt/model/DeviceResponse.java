package com.example.mqtt.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DeviceResponse {
    // Trạng thái thành công của phản hồi API
    @SerializedName("success")
    private boolean success;

    // Số lượng thiết bị trong phản hồi
    @SerializedName("count")
    private int count;

    // Danh sách các thiết bị
    @SerializedName("data")
    private List<Device> devices;

    // Getters
    public boolean isSuccess() { return success; }
    public int getCount() { return count; }
    public List<Device> getDevices() { return devices; }
}