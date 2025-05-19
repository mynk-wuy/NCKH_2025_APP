package com.example.mqtt.model;

import com.google.gson.annotations.SerializedName;

public class Device {
    // ID duy nhất của cảm biến
    @SerializedName("sensor_id")
    private String sensorId;

    // Tên của cảm biến
    @SerializedName("name_sensor")
    private String nameSensor;

    // Vị trí đặt cảm biến
    @SerializedName("location")
    private String location;

    // Loại cảm biến (ví dụ: CO, H2, NH3)
    @SerializedName("type")
    private String type;

    // Thời gian tạo cảm biến
    @SerializedName("created_at")
    private String createdAt;

    // Đơn vị đo của cảm biến (ví dụ: ppm)
    @SerializedName("donvi")
    private String donvi;

    // Ngưỡng cảnh báo của cảm biến (có thể null)
    @SerializedName("threshold")
    private Float threshold;

    // Getters
    public String getSensorId() { return sensorId; }
    public String getNameSensor() { return nameSensor; }
    public String getLocation() { return location; }
    public String getType() { return type; }
    public String getCreatedAt() { return createdAt; }
    public String getDonvi() { return donvi; }
    public Float getThreshold() { return threshold; }

    // Setters
    public void setSensorId(String sensorId) { this.sensorId = sensorId; }
    public void setNameSensor(String nameSensor) { this.nameSensor = nameSensor; }
    public void setLocation(String location) { this.location = location; }
    public void setType(String type) { this.type = type; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public void setDonvi(String donvi) { this.donvi = donvi; }
    public void setThreshold(Float threshold) { this.threshold = threshold; }
}