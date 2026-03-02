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
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SleepActivity extends BaseHealthActivity {

    private TextView tvTotalSleep;
    private TextView tvGoal;
    private EditText etInput;
    private Spinner spinnerSleepType;
    private ListView lvTodayRecords;
    private Button btnConfirm;

    private SupabaseApi apiService;
    private List<SleepLog> logDataList = new ArrayList<>();
    private SleepAdapter adapter;

    private String selectedType = "";
    private Long editingLogId = null;
    private String targetDate;

    // [자동 선택] 선택 후 돌아왔을 때 값을 기억하는 변수
    private String pendingSleepSelection = null;

    private List<SleepType> sleepTypeList = new ArrayList<>();
    private ArrayAdapter<SleepType> spinnerAdapter;

    // [Launcher] 관리 화면 결과 처리
    private final ActivityResultLauncher<Intent> sleepTypeLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String selectedName = result.getData().getStringExtra("selected_sleep_type");
                    if (selectedName != null) {
                        this.pendingSleepSelection = selectedName;
                        this.selectedType = selectedName;
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sleep);
		
		initializeUserId(); 
		
		targetDate = getTargetDateFromIntent(); 
        if (targetDate == null) {
            targetDate = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(new Date());
        }

        updateHeaderTitle();

        tvTotalSleep = findViewById(R.id.tv_total_sleep);
        tvGoal = findViewById(R.id.tv_goal);
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

        findViewById(R.id.btn_add_10).setOnClickListener(v -> addTime(10));
        findViewById(R.id.btn_add_30).setOnClickListener(v -> addTime(30));
        findViewById(R.id.btn_add_60).setOnClickListener(v -> addTime(60));
        
        findViewById(R.id.btn_reset).setOnClickListener(v -> resetUI());
        btnConfirm.setOnClickListener(v -> handleConfirmClick());

        setupGoalClickListeners();
    }
	
	private void setupGoalClickListeners() {
        tvGoal.setClickable(true);
        tvGoal.setFocusable(true);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            android.content.res.TypedArray ta = getTheme().obtainStyledAttributes(
                new int[]{android.R.attr.selectableItemBackground});
            tvGoal.setBackgroundResource(ta.getResourceId(0, 0));
            ta.recycle();
        }
        
        tvGoal.setOnClickListener(v -> {
            Toast.makeText(this, 
                "💡 목표를 길게 누르면 목표 설정 화면으로 이동합니다", 
                Toast.LENGTH_SHORT).show();
        });
        
        tvGoal.setOnLongClickListener(v -> {
            startActivity(new Intent(SleepActivity.this, SleepTargetActivity.class));
            return true;
        });
    }
	
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        targetDate = intent.getStringExtra("target_date");
        if (targetDate == null) {
            targetDate = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(new Date());
        }

        updateHeaderTitle();
        fetchTodayRecords();
    }

    private void updateHeaderTitle() {
        TextView tvHeader = findViewById(R.id.tv_record_header);
        if (tvHeader != null) {
            String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(new Date());

            if (targetDate.equals(todayStr)) {
                tvHeader.setText("오늘의 기록");
            } else {
                try {
                    SimpleDateFormat sdfInput = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
                    SimpleDateFormat sdfOutput = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA);
                    Date date = sdfInput.parse(targetDate);
                    tvHeader.setText(sdfOutput.format(date) + "의 기록");
                } catch (Exception e) {
                    tvHeader.setText(targetDate + "의 기록");
                }
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        fetchSleepTypes();
        fetchTodayRecords();
        updateGoalUI();
    }

    private void updateGoalUI() {
        SharedPreferences prefs = getSharedPreferences("HealthPrefs", Context.MODE_PRIVATE);
        int targetMinutes = prefs.getInt("sleep_target", 420);
        double hours = targetMinutes / 60.0;
        
        String text;
        if (targetMinutes % 60 == 0) {
            text = String.format(Locale.KOREA, "목표: %d분 (%d시간) 이상 ⓘ", targetMinutes, (int)hours);
        } else {
            text = String.format(Locale.KOREA, "목표: %d분 (%.1f시간) 이상 ⓘ", targetMinutes, hours);
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

        // [수정] Launcher 사용하여 관리 화면 이동
        spinnerSleepType.setOnLongClickListener(v -> {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(100);
                }
            }
            Intent intent = new Intent(SleepActivity.this, SleepTypeActivity.class);
            sleepTypeLauncher.launch(intent);
            return true;
        });
    }

    // [정렬 및 자동 선택 로직]
    private void fetchSleepTypes() {
		if (!checkNetworkAndProceed()) {
            return;
        }
        apiService.getSleepTypes().enqueue(new Callback<List<SleepType>>() {
            @Override
            public void onResponse(Call<List<SleepType>> call, Response<List<SleepType>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    sleepTypeList.clear();
                    List<SleepType> fetched = response.body();

                    // 최신순 정렬
                    Collections.sort(fetched, new Comparator<SleepType>() {
                        @Override
                        public int compare(SleepType o1, SleepType o2) {
                            return Long.compare(o2.getId(), o1.getId());
                        }
                    });

                    sleepTypeList.addAll(fetched);
                    spinnerAdapter.notifyDataSetChanged();

                    // 자동 선택 처리
                    if (pendingSleepSelection != null) {
                        for (int i = 0; i < sleepTypeList.size(); i++) {
                            if (sleepTypeList.get(i).getName().equals(pendingSleepSelection)) {
                                spinnerSleepType.setSelection(i);
                                selectedType = pendingSleepSelection;
                                break;
                            }
                        }
                        pendingSleepSelection = null;
                    } else if (!sleepTypeList.isEmpty()) {
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
		if (!checkNetworkAndProceed()) {
            return;
        }
        SleepLog newLog = new SleepLog(targetDate, selectedType, minutes, currentUserId);
        
        apiService.insertSleep(newLog).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    showSuccess("저장됨");
                    resetUI();
                    fetchTodayRecords();
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                showError("저장 실패");
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
                    showSuccess("수정됨");
                    resetUI();
                    fetchTodayRecords();
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {}
        });
    }

    private void fetchTodayRecords() {
        String query = "eq." + targetDate;
        apiService.getTodaySleepLogs(query).enqueue(new Callback<List<SleepLog>>() {
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
