package com.example.mqtt.ui.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.Manifest;
import com.example.mqtt.R;
import com.example.mqtt.databinding.FragmentHomeBinding;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.util.Log;

public class HomeFragment extends Fragment implements OnMapReadyCallback {
    private FragmentHomeBinding binding;
    private GoogleMap mMap;
    private static final int REQUEST_LOCATION_PERMISSION = 1; // Mã yêu cầu quyền vị trí
    private static final String TAG = "HomeFragment";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // Khởi tạo ViewModel cho HomeFragment
        HomeViewModel homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        // Gắn kết layout với binding
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Khởi tạo fragment bản đồ
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment == null) {
            // Nếu fragment bản đồ chưa tồn tại, tạo mới và thay thế vào layout
            mapFragment = SupportMapFragment.newInstance();
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.map, mapFragment)
                    .commit();
        }
        mapFragment.getMapAsync(this); // Gọi callback khi bản đồ sẵn sàng

        return root;
    }

    // Xử lý khi bản đồ Google sẵn sàng
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID); // Thiết lập chế độ bản đồ hybrid (kết hợp vệ tinh và đường)

        // Kiểm tra và yêu cầu quyền truy cập vị trí
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true); // Hiển thị vị trí hiện tại của người dùng
            mMap.getUiSettings().setMyLocationButtonEnabled(true); // Hiển thị nút định vị
        } else {
            // Yêu cầu quyền truy cập vị trí nếu chưa được cấp
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        }

        // Lấy roleId từ SharedPreferences để phân quyền
        SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        int roleId = prefs.getInt("role_id", 4); // Mặc định là 4 (user)

        // Tọa độ của hai trạm giám sát
        LatLng station1 = new LatLng(21.02761596021862, 105.80173122408884); // Tọa độ Trạm 1
        LatLng station2 = new LatLng(21.02726771018829, 105.80329848983965); // Tọa độ Trạm 2

        // Tạo marker cho Trạm 1
        MarkerOptions markerOptions1 = new MarkerOptions()
                .position(station1)
                .title("Trạm 1")
                .snippet("LAB MITSU") // Thông tin chi tiết khi nhấn vào marker
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)) // Màu đỏ cho marker
                .anchor(0.5f, 0.5f); // Đặt điểm neo ở giữa marker

        // Tạo marker cho Trạm 2
        MarkerOptions markerOptions2 = new MarkerOptions()
                .position(station2)
                .title("Trạm 2")
                .snippet("Tòa A5") // Thông tin chi tiết khi nhấn vào marker
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)) // Màu đỏ cho marker
                .anchor(0.5f, 0.5f); // Đặt điểm neo ở giữa marker

        // Hiển thị marker dựa trên vai trò người dùng
        if (roleId == 2) { // Admin Trạm 1
            mMap.addMarker(markerOptions1); // Chỉ hiển thị marker Trạm 1
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(station1, 18)); // Phóng to Trạm 1
        } else if (roleId == 3) { // Admin Trạm 2
            mMap.addMarker(markerOptions2); // Chỉ hiển thị marker Trạm 2
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(station2, 18)); // Phóng to Trạm 2
        } else { // Super Admin hoặc User
            // Thêm cả hai marker
            mMap.addMarker(markerOptions1);
            mMap.addMarker(markerOptions2);

            // Tạo khung nhìn bao gồm cả hai trạm
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(station1);
            builder.include(station2);
            LatLngBounds bounds = builder.build();

            // Di chuyển camera để hiển thị cả hai trạm
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 100);
            mMap.moveCamera(cu);
        }

        // Xử lý sự kiện nhấn vào marker
        mMap.setOnMarkerClickListener(marker -> {
            // Điều hướng đến DashboardFragment khi nhấn vào marker
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
            Bundle bundle = new Bundle();
            if (marker.getTitle().equals("Trạm 1")) {
                bundle.putInt("station", 1); // Gửi thông tin Trạm 1
            } else if (marker.getTitle().equals("Trạm 2")) {
                bundle.putInt("station", 2); // Gửi thông tin Trạm 2
            }
            navController.navigate(R.id.nav_dashboard, bundle);
            return true;
        });
    }

    // Xử lý kết quả yêu cầu quyền
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Quyền được cấp, kích hoạt vị trí
                if (ActivityCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                    mMap.getUiSettings().setMyLocationButtonEnabled(true);
                }
            } else {
                Log.w(TAG, "Quyền vị trí bị từ chối");
            }
        }
    }

    // Hủy binding khi fragment bị hủy
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}