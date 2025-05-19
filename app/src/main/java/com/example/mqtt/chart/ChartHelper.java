package com.example.mqtt.chart;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Environment;
import android.util.Log;
import android.widget.HorizontalScrollView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.example.mqtt.model.SensorData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChartHelper {
    private static final SimpleDateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()); // Định dạng thời gian API
    private static final SimpleDateFormat displayDateFormat = new SimpleDateFormat("HH:mm\ndd-MM", Locale.getDefault()); // Định dạng thời gian hiển thị
    private static final String TAG = "ChartHelper"; // Tag để ghi log

    // Thiết lập biểu đồ cho Trạm 1 (Lab001)
    public static void setupLab001Chart(Context context, LineChart chart, List<SensorData> lab001Data,
                                        boolean showCO, boolean showNH3, boolean showH2) {
        setupStationChart(context, chart, lab001Data, "Lab001", "Dữ liệu trạm 1", showCO, showNH3, showH2);
    }

    // Thiết lập biểu đồ cho Trạm 2 (A4002)
    public static void setupA4002Chart(Context context, LineChart chart, List<SensorData> a4002Data,
                                       boolean showCO, boolean showNH3, boolean showH2) {
        setupStationChart(context, chart, a4002Data, "A4002", "Dữ liệu trạm 2", showCO, showNH3, showH2);
    }

    // Thiết lập biểu đồ chung cho một trạm
    private static void setupStationChart(Context context, LineChart chart, List<SensorData> stationData,
                                          String stationPrefix, String description,
                                          boolean showCO, boolean showNH3, boolean showH2) {
        if (chart == null) {
            Log.e(TAG, "Biểu đồ rỗng cho " + stationPrefix);
            return;
        }

        if (stationData == null || stationData.isEmpty()) {
            chart.clear();
            chart.setNoDataText("Không có dữ liệu để hiển thị");
            Log.d(TAG, "Không có dữ liệu cho " + stationPrefix);
            return;
        }

        // Nhóm dữ liệu theo thời gian
        Map<String, Map<String, Float>> timeGroupedData = new LinkedHashMap<>();
        for (SensorData data : stationData) {
            String timeKey = data.getRecorded_at();
            String gasType = data.getSensor_id().substring(stationPrefix.length());

            timeGroupedData.computeIfAbsent(timeKey, k -> new LinkedHashMap<>())
                    .put(gasType, (float) data.getValue());
        }

        // Đảo ngược thứ tự thời gian để hiển thị mới nhất trước
        List<String> reversedTimeKeys = new ArrayList<>(timeGroupedData.keySet());
        Collections.reverse(reversedTimeKeys);

        // Chuẩn bị dữ liệu cho biểu đồ
        List<Entry> coEntries = new ArrayList<>();
        List<Entry> nh3Entries = new ArrayList<>();
        List<Entry> h2Entries = new ArrayList<>();
        List<String> timeLabels = new ArrayList<>();

        int index = 0;
        float maxYValue = Float.MIN_VALUE;
        for (String timeKey : reversedTimeKeys) {
            Map<String, Float> values = timeGroupedData.get(timeKey);

            try {
                Date date = apiDateFormat.parse(timeKey);
                String formattedTime = displayDateFormat.format(date);
                timeLabels.add(formattedTime);
                Log.d(TAG, "Thời gian định dạng: " + formattedTime + " từ " + timeKey);
            } catch (ParseException e) {
                Log.w(TAG, "Thời gian không hợp lệ: " + timeKey + ", sử dụng N/A");
                timeLabels.add("N/A");
            }

            if (values.containsKey("CO")) {
                coEntries.add(new Entry(index, values.get("CO")));
                maxYValue = Math.max(maxYValue, values.get("CO"));
            }
            if (values.containsKey("NH3")) {
                nh3Entries.add(new Entry(index, values.get("NH3")));
                maxYValue = Math.max(maxYValue, values.get("NH3"));
            }
            if (values.containsKey("H2")) {
                h2Entries.add(new Entry(index, values.get("H2")));
                maxYValue = Math.max(maxYValue, values.get("H2"));
            }
            index++;
        }

        // Tạo dữ liệu cho biểu đồ
        LineData lineData = chart.getData() != null ? chart.getData() : new LineData();
        lineData.clearValues();

        if (showCO && !coEntries.isEmpty()) lineData.addDataSet(createDataSet(coEntries, "CO", Color.RED));
        if (showNH3 && !nh3Entries.isEmpty()) lineData.addDataSet(createDataSet(nh3Entries, "NH3", Color.BLUE));
        if (showH2 && !h2Entries.isEmpty()) lineData.addDataSet(createDataSet(h2Entries, "H2", Color.GREEN));

        chart.setData(lineData);
        chart.setContentDescription("Biểu đồ " + description);

        // Cấu hình trục X
        XAxis xAxis = chart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(timeLabels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setLabelCount(Math.min(15, timeLabels.size()), true);
        xAxis.setAxisMinimum(0f);
        xAxis.setAxisMaximum(index - 1);
        xAxis.setDrawGridLines(true);
        xAxis.setAvoidFirstLastClipping(true);
        xAxis.setLabelRotationAngle(0f);
        xAxis.setTextSize(8f);

        // Cấu hình trục Y
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setGranularity(1f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(maxYValue * 1.2f);
        leftAxis.setDrawGridLines(true);
        chart.getAxisRight().setEnabled(false);

        // Cấu hình tương tác với biểu đồ
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setDoubleTapToZoomEnabled(true);
        chart.setVisibleXRangeMinimum(1f);
        chart.setVisibleXRangeMaximum(timeLabels.size());
        chart.moveViewToX(lineData.getEntryCount() - 1);

        // Tự động cuộn đến điểm mới nhất
        if (chart.getParent() != null && chart.getParent().getParent() instanceof HorizontalScrollView) {
            HorizontalScrollView scrollView = (HorizontalScrollView) chart.getParent().getParent();
            scrollView.post(() -> {
                int scrollX = chart.getWidth() - scrollView.getWidth();
                if (scrollX > 0) {
                    scrollView.smoothScrollTo(scrollX, 0);
                    Log.d(TAG, "Cuộn " + stationPrefix + " đến x=" + scrollX);
                } else {
                    Log.w(TAG, "Không cần cuộn cho " + stationPrefix + ", chiều rộng biểu đồ=" + chart.getWidth() + ", chiều rộng scrollView=" + scrollView.getWidth());
                }
            });
        } else {
            Log.w(TAG, "Không tìm thấy HorizontalScrollView cho " + stationPrefix);
        }

        chart.invalidate(); // Làm mới biểu đồ
    }

    // Lưu biểu đồ dưới dạng PNG
    public static String saveChartAsPng(LineChart chart, String fileName) {
        if (chart == null) {
            Log.e(TAG, "Biểu đồ rỗng, không thể lưu dưới dạng PNG");
            return null;
        }

        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        String filePath = new File(downloadsDir, fileName + ".png").getAbsolutePath();

        try {
            Bitmap bitmap = chart.getChartBitmap();
            FileOutputStream fos = new FileOutputStream(filePath);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            Log.d(TAG, "Biểu đồ được lưu dưới dạng PNG: " + filePath);
            return filePath;
        } catch (IOException e) {
            Log.e(TAG, "Lỗi lưu biểu đồ dưới dạng PNG: " + e.getMessage());
            return null;
        }
    }

    // Tạo tập dữ liệu cho biểu đồ
    private static LineDataSet createDataSet(List<Entry> entries, String label, int color) {
        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setColor(color);
        dataSet.setCircleColor(color);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(1f);
        dataSet.setValueTextSize(10f);
        dataSet.setDrawValues(true);
        dataSet.setMode(LineDataSet.Mode.LINEAR);
        dataSet.setValueTextColor(Color.BLACK);
        return dataSet;
    }
}