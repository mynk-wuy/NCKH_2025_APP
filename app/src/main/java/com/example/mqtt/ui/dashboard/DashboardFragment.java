package com.example.mqtt.ui.dashboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.HorizontalScrollView;
import android.widget.TabHost;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.mqtt.MainActivity;
import com.example.mqtt.R;
import com.example.mqtt.chart.ChartHelper;
import com.example.mqtt.model.SensorData;
import com.example.mqtt.model.SensorDataResponse;
import com.github.mikephil.charting.charts.LineChart;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DashboardFragment extends Fragment {
    private static final String TAG = "DashboardFragment";

    private TabHost tabHost; // TabHost để hiển thị tab Trạm 1 và Trạm 2
    private LineChart chartLab001, chartA4002; // Biểu đồ cho Trạm 1 (Lab001) và Trạm 2 (A4002)
    private TableLayout tableLab001Stats, tableA4002Stats; // Bảng thống kê dữ liệu cảm biến
    private HorizontalScrollView scrollViewChartLab001, scrollViewChartA4002; // Thanh cuộn ngang cho biểu đồ
    private CheckBox checkBoxLab001CO, checkBoxLab001NH3, checkBoxLab001H2; // Checkbox chọn loại khí cho Trạm 1
    private CheckBox checkBoxA4002CO, checkBoxA4002NH3, checkBoxA4002H2; // Checkbox chọn loại khí cho Trạm 2
    private List<SensorData> lab001Data = new ArrayList<>(); // Dữ liệu cảm biến Trạm 1
    private List<SensorData> a4002Data = new ArrayList<>(); // Dữ liệu cảm biến Trạm 2
    private boolean showLab001CO = true, showLab001NH3 = true, showLab001H2 = true; // Trạng thái hiển thị khí Trạm 1
    private boolean showA4002CO = true, showA4002NH3 = true, showA4002H2 = true; // Trạng thái hiển thị khí Trạm 2

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Gắn kết layout với fragment
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        // Khởi tạo TabHost để quản lý các tab
        tabHost = view.findViewById(android.R.id.tabhost);
        tabHost.setup();

        // Khởi tạo biểu đồ cho Trạm 1 và Trạm 2
        chartLab001 = view.findViewById(R.id.chartLab001);
        chartA4002 = view.findViewById(R.id.chartA4002);

        // Khởi tạo thanh cuộn ngang cho biểu đồ
        scrollViewChartLab001 = view.findViewById(R.id.scrollViewChartLab001);
        scrollViewChartA4002 = view.findViewById(R.id.scrollViewChartA4002);

        // Khởi tạo bảng thống kê cho Trạm 1 và Trạm 2
        tableLab001Stats = view.findViewById(R.id.tableLab001Stats);
        tableA4002Stats = view.findViewById(R.id.tableA4002Stats);

        // Khởi tạo checkbox cho các loại khí của Trạm 1
        checkBoxLab001CO = view.findViewById(R.id.checkBoxLab001CO);
        checkBoxLab001NH3 = view.findViewById(R.id.checkBoxLab001NH3);
        checkBoxLab001H2 = view.findViewById(R.id.checkBoxLab001H2);

        // Khởi tạo checkbox cho các loại khí của Trạm 2
        checkBoxA4002CO = view.findViewById(R.id.checkBoxA4002CO);
        checkBoxA4002NH3 = view.findViewById(R.id.checkBoxA4002NH3);
        checkBoxA4002H2 = view.findViewById(R.id.checkBoxA4002H2);

        // Thiết lập sự kiện khi thay đổi trạng thái checkbox của Trạm 1
        checkBoxLab001CO.setOnCheckedChangeListener((buttonView, isChecked) -> {
            showLab001CO = isChecked; // Cập nhật trạng thái hiển thị CO
            updateLab001Chart(); // Cập nhật biểu đồ Trạm 1
        });
        checkBoxLab001NH3.setOnCheckedChangeListener((buttonView, isChecked) -> {
            showLab001NH3 = isChecked; // Cập nhật trạng thái hiển thị NH3
            updateLab001Chart(); // Cập nhật biểu đồ Trạm 1
        });
        checkBoxLab001H2.setOnCheckedChangeListener((buttonView, isChecked) -> {
            showLab001H2 = isChecked; // Cập nhật trạng thái hiển thị H2
            updateLab001Chart(); // Cập nhật biểu đồ Trạm 1
        });

        // Thiết lập sự kiện khi thay đổi trạng thái checkbox của Trạm 2
        checkBoxA4002CO.setOnCheckedChangeListener((buttonView, isChecked) -> {
            showA4002CO = isChecked; // Cập nhật trạng thái hiển thị CO
            updateA4002Chart(); // Cập nhật biểu đồ Trạm 2
        });
        checkBoxA4002NH3.setOnCheckedChangeListener((buttonView, isChecked) -> {
            showA4002NH3 = isChecked; // Cập nhật trạng thái hiển thị NH3
            updateA4002Chart(); // Cập nhật biểu đồ Trạm 2
        });
        checkBoxA4002H2.setOnCheckedChangeListener((buttonView, isChecked) -> {
            showA4002H2 = isChecked; // Cập nhật trạng thái hiển thị H2
            updateA4002Chart(); // Cập nhật biểu đồ Trạm 2
        });

        // Lấy roleId từ SharedPreferences để phân quyền hiển thị
        SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        int roleId = prefs.getInt("role_id", 4); // Mặc định là 4 (user)

        // Thêm tab và ẩn nội dung dựa trên vai trò
        if (roleId == 2) {
            // Chỉ hiển thị Tab Trạm 1 (Lab001) và ẩn Trạm 2
            tabHost.addTab(tabHost.newTabSpec("tab1")
                    .setIndicator("Trạm 1")
                    .setContent(R.id.tab1));
            chartA4002.setVisibility(View.GONE); // Ẩn biểu đồ Trạm 2
            tableA4002Stats.setVisibility(View.GONE); // Ẩn bảng thống kê Trạm 2
            checkBoxA4002CO.setVisibility(View.GONE); // Ẩn checkbox CO Trạm 2
            checkBoxA4002NH3.setVisibility(View.GONE); // Ẩn checkbox NH3 Trạm 2
            checkBoxA4002H2.setVisibility(View.GONE); // Ẩn checkbox H2 Trạm 2
            if (scrollViewChartA4002 != null) scrollViewChartA4002.setVisibility(View.GONE); // Ẩn thanh cuộn Trạm 2
        } else if (roleId == 3) {
            // Chỉ hiển thị Tab Trạm 2 (A4002) và ẩn Trạm 1
            tabHost.addTab(tabHost.newTabSpec("tab2")
                    .setIndicator("Trạm 2")
                    .setContent(R.id.tab2));
            chartLab001.setVisibility(View.GONE); // Ẩn biểu đồ Trạm 1
            tableLab001Stats.setVisibility(View.GONE); // Ẩn bảng thống kê Trạm 1
            checkBoxLab001CO.setVisibility(View.GONE); // Ẩn checkbox CO Trạm 1
            checkBoxLab001NH3.setVisibility(View.GONE); // Ẩn checkbox NH3 Trạm 1
            checkBoxLab001H2.setVisibility(View.GONE); // Ẩn checkbox H2 Trạm 1
            if (scrollViewChartLab001 != null) scrollViewChartLab001.setVisibility(View.GONE); // Ẩn thanh cuộn Trạm 1
        } else {
            // Super Admin (1) hoặc User (4): hiển thị cả hai tab
            tabHost.addTab(tabHost.newTabSpec("tab1")
                    .setIndicator("Trạm 1")
                    .setContent(R.id.tab1));
            tabHost.addTab(tabHost.newTabSpec("tab2")
                    .setIndicator("Trạm 2")
                    .setContent(R.id.tab2));
        }

        // Chọn tab đầu tiên làm mặc định
        tabHost.setCurrentTab(0);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Kiểm tra tham số truyền vào để chọn tab
        Bundle args = getArguments();
        if (args != null && args.containsKey("selected_tab")) {
            int selectedTab = args.getInt("selected_tab", -1);
            Log.d(TAG, "Đặt tab được chọn: " + selectedTab);

            if (selectedTab >= 0 && selectedTab < tabHost.getTabWidget().getChildCount()) {
                tabHost.setCurrentTab(selectedTab); // Chuyển đến tab được chỉ định
            }
        }

        // Lấy dữ liệu cảm biến từ MainActivity
        if (getActivity() != null && getActivity() instanceof MainActivity) {
            Log.d(TAG, "Đang lấy dữ liệu cảm biến từ MainActivity");
            SensorDataResponse data = ((MainActivity) getActivity()).getSensorData();
            if (data != null) {
                updateData(data); // Cập nhật dữ liệu lên giao diện
            } else {
                Log.w(TAG, "Dữ liệu cảm biến rỗng");
            }
        }
    }

    // Cập nhật dữ liệu cảm biến lên giao diện
    public void updateData(SensorDataResponse response) {
        if (response != null && response.getData() != null) {
            // Lấy roleId để quyết định dữ liệu nào cần hiển thị
            SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
            int roleId = prefs.getInt("role_id", 4); // Mặc định là 4 (user)

            List<SensorData> allData = response.getData();
            lab001Data.clear(); // Xóa dữ liệu cũ của Trạm 1
            a4002Data.clear(); // Xóa dữ liệu cũ của Trạm 2

            // Phân loại dữ liệu theo trạm (Lab001 hoặc A4002)
            for (SensorData data : allData) {
                if (data.getSensor_id().contains("Lab001")) {
                    lab001Data.add(data); // Thêm dữ liệu vào Trạm 1
                } else if (data.getSensor_id().contains("A4002")) {
                    a4002Data.add(data); // Thêm dữ liệu vào Trạm 2
                }
            }

            Log.d(TAG, "Kích thước dữ liệu Lab001: " + lab001Data.size() + ", A4002: " + a4002Data.size());

            // Cập nhật biểu đồ và bảng thống kê dựa trên vai trò
            if (roleId == 2) {
                // Chỉ cập nhật dữ liệu cho Trạm 1
                if (chartLab001 != null && chartLab001.getVisibility() == View.VISIBLE) {
                    updateLab001Chart(); // Cập nhật biểu đồ Trạm 1
                }
                if (tableLab001Stats != null && tableLab001Stats.getVisibility() == View.VISIBLE) {
                    updateStationStats(lab001Data, "Lab001", tableLab001Stats); // Cập nhật bảng thống kê Trạm 1
                }
            } else if (roleId == 3) {
                // Chỉ cập nhật dữ liệu cho Trạm 2
                if (chartA4002 != null && chartA4002.getVisibility() == View.VISIBLE) {
                    updateA4002Chart(); // Cập nhật biểu đồ Trạm 2
                }
                if (tableA4002Stats != null && tableA4002Stats.getVisibility() == View.VISIBLE) {
                    updateStationStats(a4002Data, "A4002", tableA4002Stats); // Cập nhật bảng thống kê Trạm 2
                }
            } else {
                // Super Admin (1) hoặc User (4): cập nhật cả hai trạm
                if (chartLab001 != null) {
                    updateLab001Chart(); // Cập nhật biểu đồ Trạm 1
                }
                if (chartA4002 != null) {
                    updateA4002Chart(); // Cập nhật biểu đồ Trạm 2
                }
                if (tableLab001Stats != null) {
                    updateStationStats(lab001Data, "Lab001", tableLab001Stats); // Cập nhật bảng thống kê Trạm 1
                }
                if (tableA4002Stats != null) {
                    updateStationStats(a4002Data, "A4002", tableA4002Stats); // Cập nhật bảng thống kê Trạm 2
                }
            }
        } else {
            Log.w(TAG, "Phản hồi hoặc dữ liệu rỗng");
        }
    }

    // Cập nhật biểu đồ cho Trạm 1
    private void updateLab001Chart() {
        ChartHelper.setupLab001Chart(getContext(), chartLab001, lab001Data, showLab001CO, showLab001NH3, showLab001H2);
    }

    // Cập nhật biểu đồ cho Trạm 2
    private void updateA4002Chart() {
        ChartHelper.setupA4002Chart(getContext(), chartA4002, a4002Data, showA4002CO, showA4002NH3, showA4002H2);
    }

    // Tạo một hàng trong bảng thống kê
    private TableRow createTableRow(String col1, String col2, String col3, boolean isHeader) {
        TableRow row = new TableRow(getContext());

        // Cấu hình chung
        int padding = getResources().getDimensionPixelSize(R.dimen.table_cell_padding);
        TextView textView1 = new TextView(getContext());
        TextView textView2 = new TextView(getContext());
        TextView textView3 = new TextView(getContext());

        // Thiết lập style dựa trên việc có phải hàng tiêu đề không
        if (isHeader) {
            textView1.setTextAppearance(getContext(), R.style.TableHeaderText);
            textView2.setTextAppearance(getContext(), R.style.TableHeaderText);
            textView3.setTextAppearance(getContext(), R.style.TableHeaderText);
            row.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.table_header_bg)); // Màu nền tiêu đề
        } else {
            textView1.setTextAppearance(getContext(), R.style.TableCellText);
            textView2.setTextAppearance(getContext(), R.style.TableCellText);
            textView3.setTextAppearance(getContext(), R.style.TableCellText);
        }

        // Thiết lập nội dung cho các ô
        textView1.setText(col1);
        textView2.setText(col2);
        textView3.setText(col3);

        // Thiết lập padding cho các ô
        textView1.setPadding(padding, padding, padding, padding);
        textView2.setPadding(padding, padding, padding, padding);
        textView3.setPadding(padding, padding, padding, padding);

        // Thêm các ô vào hàng
        row.addView(textView1);
        row.addView(textView2);
        row.addView(textView3);

        return row;
    }

    // Cập nhật bảng thống kê cho một trạm
    private void updateStationStats(List<SensorData> stationData, String stationPrefix, TableLayout tableLayout) {
        tableLayout.removeAllViews(); // Xóa các hàng cũ

        if (stationData == null || stationData.isEmpty()) {
            // Hiển thị thông báo nếu không có dữ liệu
            TableRow row = createTableRow("Thống kê " + stationPrefix, "Không có dữ liệu", "", false);
            tableLayout.addView(row);
            Log.d(TAG, "Không có dữ liệu thống kê cho " + stationPrefix);
            return;
        }

        // Thêm hàng tiêu đề cho bảng
        tableLayout.addView(createTableRow("Chỉ số", "Giá trị", "Thời gian", true));

        // Tạo map để lưu giá trị min/max cho từng loại khí
        Map<String, Object> minCO = new HashMap<>();
        Map<String, Object> maxCO = new HashMap<>();
        Map<String, Object> minNH3 = new HashMap<>();
        Map<String, Object> maxNH3 = new HashMap<>();
        Map<String, Object> minH2 = new HashMap<>();
        Map<String, Object> maxH2 = new HashMap<>();

        // Khởi tạo giá trị min/max
        minCO.put("value", Double.MAX_VALUE);
        maxCO.put("value", Double.MIN_VALUE);
        minNH3.put("value", Double.MAX_VALUE);
        maxNH3.put("value", Double.MIN_VALUE);
        minH2.put("value", Double.MAX_VALUE);
        maxH2.put("value", Double.MIN_VALUE);

        // Tính toán giá trị min/max cho từng loại khí
        for (SensorData data : stationData) {
            String gasType = data.getSensor_id().substring(stationPrefix.length()); // Lấy loại khí (CO, NH3, H2)
            double value = data.getValue();

            switch (gasType) {
                case "CO":
                    if (value < (Double) minCO.get("value")) {
                        minCO.put("value", value);
                        minCO.put("time", data.getRecorded_at());
                    }
                    if (value > (Double) maxCO.get("value")) {
                        maxCO.put("value", value);
                        maxCO.put("time", data.getRecorded_at());
                    }
                    break;
                case "NH3":
                    if (value < (Double) minNH3.get("value")) {
                        minNH3.put("value", value);
                        minNH3.put("time", data.getRecorded_at());
                    }
                    if (value > (Double) maxNH3.get("value")) {
                        maxNH3.put("value", value);
                        maxNH3.put("time", data.getRecorded_at());
                    }
                    break;
                case "H2":
                    if (value < (Double) minH2.get("value")) {
                        minH2.put("value", value);
                        minH2.put("time", data.getRecorded_at());
                    }
                    if (value > (Double) maxH2.get("value")) {
                        maxH2.put("value", value);
                        maxH2.put("time", data.getRecorded_at());
                    }
                    break;
            }
        }

        // Thêm dữ liệu vào bảng thống kê
        if ((Double) minCO.get("value") != Double.MAX_VALUE) {
            tableLayout.addView(createTableRow("CO Min", String.format(Locale.getDefault(), "%.2f", minCO.get("value")),
                    formatTime(minCO.get("time")), false));
            tableLayout.addView(createTableRow("CO Max", String.format(Locale.getDefault(), "%.2f", maxCO.get("value")),
                    formatTime(maxCO.get("time")), false));
            tableLayout.addView(createSeparatorRow()); // Thêm hàng phân cách
        }

        if ((Double) minNH3.get("value") != Double.MAX_VALUE) {
            tableLayout.addView(createTableRow("NH3 Min", String.format(Locale.getDefault(), "%.2f", minNH3.get("value")),
                    formatTime(minNH3.get("time")), false));
            tableLayout.addView(createTableRow("NH3 Max", String.format(Locale.getDefault(), "%.2f", maxNH3.get("value")),
                    formatTime(maxNH3.get("time")), false));
            tableLayout.addView(createSeparatorRow()); // Thêm hàng phân cách
        }

        if ((Double) minH2.get("value") != Double.MAX_VALUE) {
            tableLayout.addView(createTableRow("H2 Min", String.format(Locale.getDefault(), "%.2f", minH2.get("value")),
                    formatTime(minH2.get("time")), false));
            tableLayout.addView(createTableRow("H2 Max", String.format(Locale.getDefault(), "%.2f", maxH2.get("value")),
                    formatTime(maxH2.get("time")), false));
        }

        Log.d(TAG, "Đã cập nhật bảng thống kê cho " + stationPrefix + ", số hàng: " + tableLayout.getChildCount());
    }

    // Tạo hàng phân cách trong bảng
    private TableRow createSeparatorRow() {
        TableRow row = new TableRow(getContext());
        View separator = new View(getContext());
        separator.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, 1));
        separator.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.table_separator)); // Màu phân cách
        row.addView(separator);
        return row;
    }

    // Định dạng thời gian từ chuỗi API sang định dạng hiển thị
    private String formatTime(Object timeObj) {
        if (timeObj == null) return "N/A";
        try {
            SimpleDateFormat apiFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat displayFormat = new SimpleDateFormat("HH:mm dd/MM", Locale.getDefault());
            Date date = apiFormat.parse(timeObj.toString());
            return displayFormat.format(date); // Chuyển đổi thành định dạng HH:mm dd/MM
        } catch (Exception e) {
            Log.w(TAG, "Lỗi định dạng thời gian: " + timeObj, e);
            return timeObj.toString();
        }
    }
}