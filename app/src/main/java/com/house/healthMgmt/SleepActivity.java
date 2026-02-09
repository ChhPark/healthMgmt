package com.house.healthMgmt;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SleepActivity extends AppCompatActivity {

    private TextView tvTotalSleep;
    private TextView tvGoal; // [추가] 목표 텍스트 뷰
    private EditText etInput;
    private Spinner spinnerSleepType;
    private ListView lvTodayRecords;
    private Button btnConfirm;

    private SupabaseApi apiService;
    private List<SleepLog> logDataList = new ArrayList<>();
    private SleepAdapter adapter;

    private String selectedType = "";
    private String currentUserId = "user_01";
    private Long editingLogId = null;

    private List<SleepType> sleepTypeList = new ArrayList<>();
    private ArrayAdapter<SleepType> spinnerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sleep);

        tvTotalSleep = findViewById(R.id.tv_total_sleep);
        tvGoal = findViewById(R.id.tv_goal); // [연결]
        etInput = findViewById(R.id.et_sleep_input);
        spinnerSleepType = findViewById(R.id.spinner_sleep_type);
        lvTodayRecords = findViewById(R.id.lv_today_records);
        btnConfirm = findViewById(R.id.btn_confirm_add);

        apiService = SupabaseClient.getApi(this);

        setupSpinnerWithLongClick();

        adapter = new SleepAdapter(this, logDataList, new SleepAdapter.OnRecordActionListener() {
            @Override
            public void onEdit(SleepLog log) {
                loadRecordForEdit(log);
            }
            @Override
            public void onDelete(SleepLog log) {
                showDeleteConfirmDialog(log);
            }
        });
        lvTodayRecords.setAdapter(adapter);

        // 빠른 시간 추가 버튼
        findViewById(R.id.btn_add_10).setOnClickListener(v -> addTime(10));
        findViewById(R.id.btn_add_30).setOnClickListener(v -> addTime(30));
        findViewById(R.id.btn_add_60).setOnClickListener(v -> addTime(60));
        
        findViewById(R.id.btn_reset).setOnClickListener(v -> resetUI());
        btnConfirm.setOnClickListener(v -> handleConfirmClick());

        // [추가] 목표 텍스트 클릭 시 설정 화면으로 이동
        tvGoal.setOnClickListener(v -> {
            startActivity(new Intent(SleepActivity.this, SleepTargetActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchSleepTypes();
        fetchTodayRecords();
        updateGoalUI(); // [추가] 화면 돌아올 때 목표 텍스트 갱신
    }

    // 목표 텍스트 갱신 메서드
    private void updateGoalUI() {
        SharedPreferences prefs = getSharedPreferences("HealthPrefs", Context.MODE_PRIVATE);
        int targetMinutes = prefs.getInt("sleep_target", 420); // 기본값 420분 (7시간)
        double hours = targetMinutes / 60.0;
        
        // 예: "목표: 420분 (7시간) 이상"
        String text;
        if (targetMinutes % 60 == 0) {
            text = String.format(Locale.KOREA, "목표: %d분 (%d시간) 이상", targetMinutes, (int)hours);
        } else {
            text = String.format(Locale.KOREA, "목표: %d분 (%.1f시간) 이상", targetMinutes, hours);
        }
        tvGoal.setText(text);
    }

    private void setupSpinnerWithLongClick() {
        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sleepTypeList);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSleepType.setAdapter(spinnerAdapter);

        spinnerSleepType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!sleepTypeList.isEmpty()) selectedType = sleepTypeList.get(position).getName();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerSleepType.setOnLongClickListener(v -> {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(100);
                }
            }
            startActivity(new Intent(SleepActivity.this, SleepTypeActivity.class));
            return true;
        });
    }

    private void fetchSleepTypes() {
        apiService.getSleepTypes().enqueue(new Callback<List<SleepType>>() {
            @Override
            public void onResponse(Call<List<SleepType>> call, Response<List<SleepType>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    sleepTypeList.clear();
                    sleepTypeList.addAll(response.body());
                    spinnerAdapter.notifyDataSetChanged();

                    if (!sleepTypeList.isEmpty()) {
                        boolean found = false;
                        for (int i = 0; i < sleepTypeList.size(); i++) {
                            if (sleepTypeList.get(i).getName().equals(selectedType)) {
                                spinnerSleepType.setSelection(i);
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            selectedType = sleepTypeList.get(0).getName();
                            spinnerSleepType.setSelection(0);
                        }
                    } else {
                        selectedType = "";
                    }
                }
            }
            @Override
            public void onFailure(Call<List<SleepType>> call, Throwable t) {}
        });
    }

    private void handleConfirmClick() {
        String inputStr = etInput.getText().toString();
        if (inputStr.isEmpty()) return;

        try {
            int minutes = Integer.parseInt(inputStr.replace(",", ""));
            if (editingLogId == null) saveRecordToServer(minutes);
            else updateRecordToServer(editingLogId, minutes);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "숫자만 입력 가능합니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadRecordForEdit(SleepLog log) {
        editingLogId = log.getId();
        etInput.setText(String.valueOf(log.getMinutes()));
        for (int i = 0; i < sleepTypeList.size(); i++) {
            if (sleepTypeList.get(i).getName().equals(log.getSleepType())) {
                spinnerSleepType.setSelection(i);
                break;
            }
        }
        btnConfirm.setText("수정");
    }

    private void resetUI() {
        etInput.setText("");
        editingLogId = null;
        btnConfirm.setText("추가");
        if (spinnerSleepType.getCount() > 0) spinnerSleepType.setSelection(0);
    }

    private void saveRecordToServer(int minutes) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(new Date());
        SleepLog newLog = new SleepLog(today, selectedType, minutes, currentUserId);
        
        apiService.insertSleep(newLog).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(SleepActivity.this, "저장됨", Toast.LENGTH_SHORT).show();
                    resetUI();
                    fetchTodayRecords();
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(SleepActivity.this, "저장 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateRecordToServer(long id, int newMinutes) {
        Map<String, Object> updateFields = new HashMap<>();
        updateFields.put("minutes", newMinutes);
        updateFields.put("sleep_type", selectedType);

        apiService.updateSleep("eq." + id, updateFields).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(SleepActivity.this, "수정됨", Toast.LENGTH_SHORT).show();
                    resetUI();
                    fetchTodayRecords();
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {}
        });
    }

    private void fetchTodayRecords() {
        String today = "eq." + new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(new Date());
        apiService.getTodaySleepLogs(today).enqueue(new Callback<List<SleepLog>>() {
            @Override
            public void onResponse(Call<List<SleepLog>> call, Response<List<SleepLog>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    logDataList.clear();
                    int totalMinutes = 0;
                    for (SleepLog log : response.body()) {
                        totalMinutes += log.getMinutes();
                        logDataList.add(log);
                    }
                    tvTotalSleep.setText(String.format("%,d", totalMinutes));
                    adapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onFailure(Call<List<SleepLog>> call, Throwable t) {}
        });
    }

    private void showDeleteConfirmDialog(SleepLog log) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_delete_confirm, null);
        builder.setView(view);
        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        TextView tvMessage = view.findViewById(R.id.tv_dialog_message);
        view.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btn_delete).setOnClickListener(v -> {
            apiService.deleteSleep("eq." + log.getId()).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) fetchTodayRecords();
                }
                @Override
                public void onFailure(Call<Void> call, Throwable t) {}
            });
            dialog.dismiss();
        });
        tvMessage.setText("기록 삭제?");
        dialog.show();
    }

    private void addTime(int minutes) {
        String currentText = etInput.getText().toString().replace(",", "");
        int currentVal = 0;
        try { 
            if (!currentText.isEmpty()) currentVal = Integer.parseInt(currentText); 
        } catch (Exception e) {}
        
        int newVal = currentVal + minutes;
        etInput.setText(String.valueOf(newVal));
    }

    private static class SleepAdapter extends ArrayAdapter<SleepLog> {
        private Context context;
        private List<SleepLog> logs;
        private OnRecordActionListener listener;

        public interface OnRecordActionListener {
            void onEdit(SleepLog log);
            void onDelete(SleepLog log);
        }

        public SleepAdapter(@NonNull Context context, List<SleepLog> logs, OnRecordActionListener listener) {
            super(context, 0, logs);
            this.context = context;
            this.logs = logs;
            this.listener = listener;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_protein_record, parent, false);
            }

            SleepLog log = logs.get(position);

            TextView tvName = convertView.findViewById(R.id.tv_food_name);
            TextView tvAmount = convertView.findViewById(R.id.tv_protein_amt);
            TextView tvUnit = convertView.findViewById(R.id.tv_unit);
            View colorBar = convertView.findViewById(R.id.v_color_bar);

            int indigoColor = Color.parseColor("#3F51B5");

            if (colorBar != null) colorBar.setBackgroundColor(indigoColor);
            
            if (tvAmount != null) {
                tvAmount.setText(String.valueOf(log.getMinutes()));
                tvAmount.setTextColor(indigoColor);
            }
            if (tvUnit != null) {
                tvUnit.setText("분");
                tvUnit.setTextColor(indigoColor);
            }
            if (tvName != null) {
                tvName.setText(log.getSleepType());
            }

            View btnEdit = convertView.findViewById(R.id.iv_edit);
            View btnDelete = convertView.findViewById(R.id.iv_delete);

            if (btnEdit != null) btnEdit.setOnClickListener(v -> listener.onEdit(log));
            if (btnDelete != null) btnDelete.setOnClickListener(v -> listener.onDelete(log));

            return convertView;
        }
    }
}
