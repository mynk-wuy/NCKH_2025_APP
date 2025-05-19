package com.example.mqtt.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class UserResponse {
    // Số lượng người dùng trong phản hồi
    @SerializedName("count")
    private int count;

    // Trạng thái thành công của phản hồi API
    @SerializedName("success")
    private boolean success;

    // Danh sách người dùng
    @SerializedName("users")
    private List<User> users;

    // Khởi tạo danh sách người dùng để tránh null
    public UserResponse() {
        this.users = new ArrayList<>();
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    // Trả về danh sách người dùng, nếu null thì trả về danh sách rỗng
    public List<User> getUsers() {
        return users != null ? users : new ArrayList<>();
    }

    public void setUsers(List<User> users) {
        this.users = users != null ? users : new ArrayList<>();
    }

    // Chuyển đổi User sang đối tượng dùng cho đăng nhập
    public static User convertToLoginUser(User user) {
        User loginUser = new User();
        loginUser.setUserId(user.getUserId());
        loginUser.setUsername(user.getUsername());
        loginUser.setEmail(user.getEmail());
        loginUser.setRoleId(user.getRoleId()); // roleId là int, không cần kiểm tra null
        loginUser.setPassword(user.getPassword());
        return loginUser;
    }

    // Chuyển đổi User sang đối tượng dùng cho quản lý
    public static User convertToManagementUser(User user) {
        User managementUser = new User();
        managementUser.setUserId(user.getUserId());
        managementUser.setUsername(user.getUsername());
        managementUser.setEmail(user.getEmail());
        managementUser.setRoleName(user.getRoleName());
        managementUser.setCreatedAt(user.getCreatedAt());
        return managementUser;
    }
}