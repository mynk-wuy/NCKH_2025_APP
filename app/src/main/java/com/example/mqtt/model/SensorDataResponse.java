package com.example.mqtt.model;

import java.util.List;

public class SensorDataResponse {
    // Trạng thái thành công của phản hồi API
    private boolean success;

    // Số lượng bản ghi dữ liệu cảm biến
    private int count;

    // Danh sách dữ liệu cảm biến
    private List<SensorData> data;

    // Getter và Setter
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<SensorData> getData() {
        return data;
    }

    public void setData(List<SensorData> data) {
        this.data = data;
    }
}