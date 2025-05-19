package com.example.mqtt.connectMQTT;

import android.content.Context;
import android.util.Log;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class MqttManager {
    private static final String TAG = "MqttManager"; // Tag để ghi log
    private static MqttManager instance; // Instance singleton
    private Mqtt3AsyncClient client; // Client MQTT
    private Context context; // Context ứng dụng

    private Map<String, Consumer<String>> topicCallbacks = new HashMap<>(); // Lưu callback cho các topic
    private Map<String, Integer> topicSubscriberCounts = new HashMap<>(); // Đếm số lượng subscriber cho mỗi topic

    private MqttManager(Context context) {
        this.context = context.getApplicationContext();
        initializeClient(); // Khởi tạo client MQTT
    }

    // Lấy instance singleton của MqttManager
    public static synchronized MqttManager getInstance(Context context) {
        if (instance == null) {
            instance = new MqttManager(context);
        }
        return instance;
    }

    // Khởi tạo client MQTT
    private void initializeClient() {
        client = MqttClient.builder()
                .useMqttVersion3()
                .identifier(MqttConfig.CLIENT_ID + UUID.randomUUID().toString().substring(0, 5)) // Tạo ID client duy nhất
                .serverHost(MqttConfig.SERVER_URI.split("://")[1].split(":")[0]) // Lấy host từ URI
                .serverPort(Integer.parseInt(MqttConfig.SERVER_URI.split(":")[2])) // Lấy cổng
                .sslWithDefaultConfig() // Sử dụng SSL
                .buildAsync();
    }

    // Kết nối đến máy chủ MQTT
    public void connect(Consumer<Boolean> connectionCallback) {
        client.connectWith()
                .simpleAuth()
                .username(MqttConfig.USERNAME)
                .password(MqttConfig.PASSWORD.getBytes())
                .applySimpleAuth()
                .send()
                .whenComplete((connAck, throwable) -> {
                    if (throwable != null) {
                        Log.e(TAG, "Kết nối thất bại: " + throwable.getMessage());
                        connectionCallback.accept(false);
                    } else {
                        Log.d(TAG, "Kết nối MQTT thành công");
                        connectionCallback.accept(true);
                    }
                });
    }

    // Ngắt kết nối MQTT
    public void disconnect() {
        if (client != null) {
            client.disconnect()
                    .whenComplete((voidResult, throwable) -> {
                        if (throwable != null) {
                            Log.e(TAG, "Ngắt kết nối thất bại: " + throwable.getMessage());
                        } else {
                            Log.d(TAG, "Ngắt kết nối MQTT thành công");
                        }
                    });
        }
    }

    // Kiểm tra trạng thái kết nối
    public boolean isConnected() {
        return client != null && client.getState().isConnected();
    }

    // Đăng ký topic với callback xử lý tin nhắn
    public void subscribe(String topic, int qos, Consumer<String> callback) {
        if (!isConnected()) {
            Log.w(TAG, "MQTT chưa kết nối, không thể đăng ký topic: " + topic);
            return;
        }

        // Tăng số lượng subscriber cho topic
        topicSubscriberCounts.put(topic, topicSubscriberCounts.getOrDefault(topic, 0) + 1);
        topicCallbacks.put(topic, callback);

        client.subscribeWith()
                .topicFilter(topic)
                .qos(MqttQos.fromCode(qos))
                .callback(publish -> {
                    String message = new String(publish.getPayloadAsBytes());
                    Log.d(TAG, "Nhận tin nhắn từ topic " + topic + ": " + message);
                    Consumer<String> topicCallback = topicCallbacks.get(topic);
                    if (topicCallback != null) {
                        topicCallback.accept(message);
                    }
                })
                .send()
                .whenComplete((subAck, throwable) -> {
                    if (throwable != null) {
                        Log.e(TAG, "Đăng ký topic " + topic + " thất bại: " + throwable.getMessage());
                    } else {
                        Log.d(TAG, "Đăng ký topic " + topic + " thành công");
                    }
                });
    }

    // Hủy đăng ký topic
    public void unsubscribe(String topic) {
        if (!isConnected()) {
            Log.w(TAG, "MQTT chưa kết nối, không thể hủy đăng ký topic: " + topic);
            return;
        }

        // Giảm số lượng subscriber
        int count = topicSubscriberCounts.getOrDefault(topic, 0);
        if (count <= 1) {
            client.unsubscribeWith()
                    .topicFilter(topic)
                    .send()
                    .whenComplete((unsubAck, throwable) -> {
                        if (throwable != null) {
                            Log.e(TAG, "Hủy đăng ký topic " + topic + " thất bại: " + throwable.getMessage());
                        } else {
                            Log.d(TAG, "Hủy đăng ký topic " + topic + " thành công");
                            topicCallbacks.remove(topic);
                            topicSubscriberCounts.remove(topic);
                        }
                    });
        } else {
            topicSubscriberCounts.put(topic, count - 1);
            Log.d(TAG, "Giảm số lượng subscriber cho topic " + topic + " xuống " + (count - 1));
        }
    }

    // Gửi tin nhắn đến topic
    public void publish(String topic, String message) {
        if (!isConnected()) {
            Log.w(TAG, "MQTT chưa kết nối, không thể gửi tin nhắn đến topic: " + topic);
            return;
        }

        client.publishWith()
                .topic(topic)
                .qos(MqttQos.AT_LEAST_ONCE)
                .payload(message.getBytes())
                .send()
                .whenComplete((publish, throwable) -> {
                    if (throwable != null) {
                        Log.e(TAG, "Gửi tin nhắn đến topic " + topic + " thất bại: " + throwable.getMessage());
                    } else {
                        Log.d(TAG, "Gửi tin nhắn đến topic " + topic + ": " + message);
                    }
                });
    }

    // Hủy client khi không cần thiết
    public void destroy() {
        disconnect();
        topicCallbacks.clear();
        topicSubscriberCounts.clear();
        instance = null;
        Log.d(TAG, "Hủy MqttManager");
    }
}