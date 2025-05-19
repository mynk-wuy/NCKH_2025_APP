package com.example.mqtt.model;

public class SensorData {
    // ID duy nhất của dữ liệu cảm biến
    private double data_id;

    // Thời gian ghi nhận dữ liệu
    private String recorded_at;

    // ID của cảm biến
    private String sensor_id;

    // Giá trị đo được từ cảm biến
    private double value;

    // Getter và Setter
    public double getData_id() {
        return data_id;
    }

    public void setData_id(double data_id) {
        this.data_id = data_id;
    }

    public String getRecorded_at() {
        return recorded_at;
    }

    public void setRecorded_at(String recorded_at) {
        this.recorded_at = recorded_at;
    }

    public String getSensor_id() {
        return sensor_id;
    }

    public void setSensor_id(String sensor_id) {
        this.sensor_id = sensor_id;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }
}