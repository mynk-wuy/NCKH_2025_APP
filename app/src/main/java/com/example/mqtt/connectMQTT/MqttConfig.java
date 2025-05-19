package com.example.mqtt.connectMQTT;

import java.util.UUID;

public class MqttConfig {
    // Cấu hình đăng nhập MQTT
    public static final String SERVER_URI = "ssl://fd553ba641bf43729bad8a7af8400930.s1.eu.hivemq.cloud:8883"; // URI máy chủ MQTT
    public static final String USERNAME = "NCKH2025"; // Tên người dùng
    public static final String PASSWORD = "Manh09092003"; // Mật khẩu
    public static final String CLIENT_ID = "AndroidClient_" + UUID.randomUUID().toString().substring(0, 8); // ID client duy nhất
    public static final String TOPIC_REQUEST_STATUS = "system/request_status"; // Topic yêu cầu trạng thái hệ thống
    public static final String TOPIC_REQUEST_STATUS_1 = "Lab001/request_status"; // Topic yêu cầu trạng thái Trạm 1
    public static final String TOPIC_REQUEST_STATUS_2 = "A4002/request_status"; // Topic yêu cầu trạng thái Trạm 2

    // Các topic MQTT cho Trạm 2 (A4002)
    public static final String TOPIC_SENSOR_DATA2 = "A4002/sensors/data"; // Topic dữ liệu cảm biến
    public static final String TOPIC_FAN_CONTROL2 = "A4002/fan/control"; // Topic điều khiển quạt
    public static final String TOPIC_LED_CONTROL2 = "A4002/led/control"; // Topic điều khiển đèn
    public static final String TOPIC_FAN_STATUS2 = "A4002/fan/status"; // Topic trạng thái quạt
    public static final String TOPIC_LED_STATUS2 = "A4002/led/status"; // Topic trạng thái đèn
    public static final String TOPIC_MODE_CONTROL2 = "A4002/mode/control"; // Topic điều khiển chế độ
    public static final String TOPIC_MODE_STATUS2 = "A4002/mode/status"; // Topic trạng thái chế độ

    // Các topic MQTT cho Trạm 1 (Lab001)
    public static final String TOPIC_SENSOR_DATA1 = "Lab001/sensors/data";
    public static final String TOPIC_FAN_CONTROL1 = "Lab001/fan/control";
    public static final String TOPIC_LED_CONTROL1 = "Lab001/led/control";
    public static final String TOPIC_FAN_STATUS1 = "Lab001/fan/status";
    public static final String TOPIC_LED_STATUS1 = "Lab001/led/status";
    public static final String TOPIC_MODE_CONTROL1 = "Lab001/mode/control";
    public static final String TOPIC_MODE_STATUS1 = "Lab001/mode/status";
}