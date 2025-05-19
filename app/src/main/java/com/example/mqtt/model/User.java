package com.example.mqtt.model;

import com.google.gson.annotations.SerializedName;

public class User {
    // ID duy nhất của người dùng
    @SerializedName("user_id")
    private int userId;

    // Tên đăng nhập
    @SerializedName("username")
    private String username;

    // Địa chỉ email
    @SerializedName("email")
    private String email;

    // ID vai trò của người dùng (1: Super Admin, 2: Admin Trạm 1, 3: Admin Trạm 2)
    @SerializedName("role_id")
    private int roleId; // Sử dụng int thay vì Integer để tránh null

    // Mật khẩu
    @SerializedName("password")
    private String password;

    // Tên vai trò (ví dụ: Super Admin)
    @SerializedName("role_name")
    private String roleName;

    // Thời gian tạo tài khoản
    @SerializedName("created_at")
    private String createdAt;

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getRoleId() {
        return roleId;
    }

    public void setRoleId(int roleId) {
        this.roleId = roleId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}