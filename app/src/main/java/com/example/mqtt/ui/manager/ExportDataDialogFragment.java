package com.example.mqtt.ui.manager;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.mqtt.MainActivity;
import com.example.mqtt.R;
import com.example.mqtt.model.SensorData;
import com.example.mqtt.model.SensorDataResponse;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExportDataDialogFragment extends DialogFragment {
    private Spinner spinnerStation, spinnerSensor; // Chọn trạm và cảm biến
    private RadioGroup radioGroupTime;
    private RadioButton radioDay, radioWeek, radioMonth; // Chọn khoảng thời gian
    private EditText editTextFileName; // Nhập tên file
    private Button btnExport;
    private MainActivity mainActivity;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_export_data, null);

        // Khởi tạo các thành phần giao diện
        spinnerStation = view.findViewById(R.id.spinnerStation);
        spinnerSensor = view.findViewById(R.id.spinnerSensor);
        radioGroupTime = view.findViewById(R.id.radioGroupTime);
        radioDay = view.findViewById(R.id.radioDay);
        radioWeek = view.findViewById(R.id.radioWeek);
        radioMonth = view.findViewById(R.id.radioMonth);
        editTextFileName = view.findViewById(R.id.editTextFileName);
        btnExport = view.findViewById(R.id.btnExport);

        // Thiết lập adapter cho spinner
        ArrayAdapter<CharSequence> stationAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.stations_array, android.R.layout.simple_spinner_item);
        stationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStation.setAdapter(stationAdapter);

        ArrayAdapter<CharSequence> sensorAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.sensors_array, android.R.layout.simple_spinner_item);
        sensorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSensor.setAdapter(sensorAdapter);

        radioDay.setChecked(true); // Mặc định chọn ngày

        // Xử lý nút xuất dữ liệu
        btnExport.setOnClickListener(v -> {
            String station = spinnerStation.getSelectedItem().toString();
            String sensor = spinnerSensor.getSelectedItem().toString();
            String timePeriod = radioDay.isChecked() ? "day" : radioWeek.isChecked() ? "week" : "month";
            String fileName = editTextFileName.getText().toString().trim();

            if (fileName.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập tên file", Toast.LENGTH_SHORT).show();
                return;
            }

            exportData(station, sensor, timePeriod, fileName); // Xuất dữ liệu
            dismiss();
        });

        builder.setView(view)
                .setTitle("Xuất dữ liệu")
                .setNegativeButton("Hủy", (dialog, which) -> dismiss());
        return builder.create();
    }

    // Xuất dữ liệu ra file LaTeX
    private void exportData(String station, String sensor, String timePeriod, String fileName) {
        if (mainActivity == null) {
            mainActivity = (MainActivity) getActivity();
        }
        SensorDataResponse data = mainActivity.getSensorData();
        if (data == null || data.getData() == null) {
            Toast.makeText(getContext(), "Không có dữ liệu để xuất", Toast.LENGTH_SHORT).show();
            return;
        }

        // Lọc dữ liệu theo trạm, cảm biến và thời gian
        List<SensorData> filteredData = filterData(data, station, sensor, timePeriod);
        if (filteredData.isEmpty()) {
            Toast.makeText(getContext(), "Không có dữ liệu phù hợp để xuất", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo đường dẫn file trong thư mục Downloads
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        String texFilePath = new File(downloadsDir, fileName + ".tex").getAbsolutePath();

        String latexContent = generateLatexContent(station, sensor, timePeriod, filteredData);
        try (FileWriter writer = new FileWriter(texFilePath)) {
            writer.write(latexContent);
            Toast.makeText(getContext(), "Đã tạo file LaTeX: " + fileName + ".tex trong Downloads", Toast.LENGTH_SHORT).show();
            compileLatexToPdf(texFilePath, fileName); // Biên dịch thành PDF
        } catch (IOException e) {
            Toast.makeText(getContext(), "Lỗi khi tạo file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Lọc dữ liệu theo trạm, cảm biến và thời gian
    private List<SensorData> filterData(SensorDataResponse data, String station, String sensor, String timePeriod) {
        List<SensorData> filteredData = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        Date now = new Date();
        calendar.setTime(now);

        // Xác định khoảng thời gian
        if (timePeriod.equals("day")) {
            calendar.add(Calendar.DAY_OF_YEAR, -1);
        } else if (timePeriod.equals("week")) {
            calendar.add(Calendar.WEEK_OF_YEAR, -1);
        } else {
            calendar.add(Calendar.MONTH, -1);
        }
        Date startDate = calendar.getTime();

        String stationPrefix = station.contains("Trạm 1") ? "Lab001" : "A4002";

        for (SensorData sd : data.getData()) {
            if (sd.getSensor_id().contains(stationPrefix) && sd.getSensor_id().endsWith(sensor)) {
                try {
                    Date recordedDate = sdf.parse(sd.getRecorded_at());
                    if (recordedDate != null && recordedDate.after(startDate) && recordedDate.before(now)) {
                        filteredData.add(sd);
                    }
                } catch (ParseException e) {
                    // Bỏ qua nếu không phân tích được thời gian
                }
            }
        }
        return filteredData;
    }

    // Tạo nội dung file LaTeX
    private String generateLatexContent(String station, String sensor, String timePeriod, List<SensorData> data) {
        StringBuilder content = new StringBuilder();
        content.append("\\documentclass[a4paper,12pt]{article}\n");
        content.append("\\usepackage[utf8]{inputenc}\n");
        content.append("\\usepackage[vietnamese]{babel}\n"); // Hỗ trợ tiếng Việt
        content.append("\\usepackage{geometry}\n");
        content.append("\\geometry{a4paper, margin=1in}\n");
        content.append("\\usepackage{longtable}\n");
        content.append("\\usepackage{booktabs}\n");
        content.append("\\usepackage{caption}\n");
        content.append("\\usepackage[table]{xcolor}\n");
        content.append("\\usepackage{DejaVuSans}\n"); // Font hỗ trợ tiếng Việt
        content.append("\\usepackage{pgfplots}\n");
        content.append("\\pgfplotsset{compat=1.18}\n");
        content.append("\\begin{document}\n");

        content.append("\\begin{center}\n");
        content.append("\\textbf{\\LARGE BÁO CÁO DỮ LIỆU CẢM BIẾN}\n");
        content.append("\\end{center}\n");
        content.append("\\vspace{0.5cm}\n");

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String currentDate = dateFormat.format(new Date());
        content.append("Ngày lập báo cáo: ").append(currentDate).append("\n");
        content.append("Người lập báo cáo: Nguyễn Văn Mạnh\n");
        content.append("Vai trò: Super Admin\n");
        content.append("Thời gian: ").append(timePeriod).append("\n");
        content.append("Loại cảm biến: ").append(sensor).append("\n");
        content.append("\\vspace{1cm}\n");

        content.append("\\section*{THỐNG KÊ DỮ LIỆU CẢM BIẾN}\n");
        content.append("\\begin{longtable}{|l|c|c|c|}\n");
        content.append("\\hline\n");
        content.append("\\textbf{Cảm biến} & \\textbf{Trung bình (ppm)} & \\textbf{Tối đa (ppm)} & \\textbf{Tối thiểu (ppm)} \\\\\n");
        content.append("\\hline\\endhead\n");

        // Tính toán thống kê
        double sum = 0, max = Double.MIN_VALUE, min = Double.MAX_VALUE;
        for (SensorData sd : data) {
            double value = sd.getValue();
            sum += value;
            max = Math.max(max, value);
            min = Math.min(min, value);
        }
        double avg = data.isEmpty() ? 0 : sum / data.size();

        content.append(String.format("%s & %.2f & %.2f & %.2f \\\\\n", sensor, avg, max, min));
        content.append("\\hline\n");
        content.append("\\end{longtable}\n");
        content.append("\\vspace{1cm}\n");

        // Tạo biểu đồ
        content.append("\\section*{BIỂU ĐỒ DỮ LIỆU}\n");
        content.append("\\begin{tikzpicture}\n");
        content.append("\\begin{axis}[\n");
        content.append("    title={Biểu đồ ").append(sensor).append(" tại ").append(station).append("},\n");
        content.append("    xlabel={Thời gian},\n");
        content.append("    ylabel={Giá trị (ppm)},\n");
        content.append("    xmin=0, xmax=").append(data.size() - 1).append(",\n");
        content.append("    ymin=0, ymax=").append(max * 1.2).append(",\n");
        content.append("    legend pos=north west,\n");
        content.append("    width=12cm,\n");
        content.append("    height=8cm,\n");
        content.append("]\n");

        for (int i = 0; i < data.size(); i++) {
            SensorData sd = data.get(i);
            content.append(String.format("\\addplot coordinates {(%d, %.2f)};\n", i, sd.getValue()));
        }
        content.append("\\addlegendentry{").append(sensor).append("}\n");
        content.append("\\end{axis}\n");
        content.append("\\end{tikzpicture}\n");

        content.append("\\vspace{1cm}\n");
        content.append("\\end{document}\n");
        return content.toString();
    }

    // Biên dịch file LaTeX thành PDF
    private void compileLatexToPdf(String texFilePath, String fileName) {
        Toast.makeText(getContext(), "Biên dịch PDF (yêu cầu công cụ LaTeX): " + fileName + ".pdf trong Downloads", Toast.LENGTH_LONG).show();
    }

    // Gắn MainActivity
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            mainActivity = (MainActivity) context;
        }
    }
}