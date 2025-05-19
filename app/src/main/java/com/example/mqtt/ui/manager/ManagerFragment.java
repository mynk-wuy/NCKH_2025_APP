package com.example.mqtt.ui.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.mqtt.R;

public class ManagerFragment extends Fragment {
    private Button btnAccountManagement, btnDeviceManagement, btnExportData;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manager, container, false);

        // Khởi tạo các nút
        btnAccountManagement = view.findViewById(R.id.btnAccountManagement);
        btnDeviceManagement = view.findViewById(R.id.btnDeviceManagement);
        btnExportData = view.findViewById(R.id.btnExportData);

        // Lấy roleId từ SharedPreferences
        SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        int roleId = prefs.getInt("role_id", 4); // Mặc định là 4 (user)

        // Hiển thị/ẩn nút theo vai trò
        if (roleId == 1) { // Super Admin
            btnAccountManagement.setVisibility(View.VISIBLE);
            btnDeviceManagement.setVisibility(View.VISIBLE);
            btnExportData.setVisibility(View.VISIBLE);
        } else if (roleId == 2 || roleId == 3) { // Admin Trạm 1 hoặc 2
            btnAccountManagement.setVisibility(View.GONE); // Ẩn quản lý tài khoản
            btnDeviceManagement.setVisibility(View.VISIBLE);
            btnExportData.setVisibility(View.VISIBLE);
        } else { // User
            btnAccountManagement.setVisibility(View.GONE);
            btnDeviceManagement.setVisibility(View.GONE);
            btnExportData.setVisibility(View.GONE);
        }

        // Xử lý sự kiện nút
        btnAccountManagement.setOnClickListener(v -> showAccountManagement());
        btnDeviceManagement.setOnClickListener(v -> showDeviceManagement());
        btnExportData.setOnClickListener(v -> showExportDialog());

        return view;
    }

    // Điều hướng đến màn hình quản lý tài khoản
    private void showAccountManagement() {
        NavController navController = NavHostFragment.findNavController(this);
        navController.navigate(R.id.nav_account_management);
    }

    // Điều hướng đến màn hình quản lý thiết bị
    private void showDeviceManagement() {
        NavController navController = NavHostFragment.findNavController(this);
        navController.navigate(R.id.nav_device_management);
    }

    // Hiển thị hộp thoại xuất dữ liệu
    private void showExportDialog() {
        ExportDataDialogFragment dialog = new ExportDataDialogFragment();
        dialog.show(getChildFragmentManager(), "ExportDataDialog");
    }
}