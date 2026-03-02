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

public class ExerciseActivity extends BaseHealthActivity {

    private TextView tvTotalExercise;
    private TextView tvGoal;
    private EditText etInput;
    private Spinner spinnerExerciseType;
    private ListView lvTodayRecords;
    private Button btnConfirm;

    // 빠른 추가 버튼 멤버 변수화 (동적 변경을 위해)
    private Button btnAdd1;
    private Button btnAdd10;
    private Button btnAdd30;

    private SupabaseApi apiService;
    private List<ExerciseLog> logDataList = new ArrayList<>();
    private ExerciseAdapter adapter;

    private String selectedType = "";
    private Long editingLogId = null;
    private String targetDate;

    // [자동 선택] 선택 후 돌아왔을 때 값을 기억하는 변수
    private String pendingExerciseSelection = null;

    private List<ExerciseType> exerciseTypeList = new ArrayList<>();
    private ArrayAdapter<ExerciseType> spinnerAdapter;

    // [Launcher] 관리 화면 결과 처리
    private final ActivityResultLauncher<Intent> exerciseTypeLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String selectedName = result.getData().getStringExtra("selected_exercise_type");
                    if (selectedName != null) {
                        this.pendingExerciseSelection = selectedName;
                        this.selectedType = selectedName;
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise);
		
        initializeUserId();
		
        targetDate = getTargetDateFromIntent();
        if (targetDate == null) {
            targetDate = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(new Date());
        }

        updateHeaderTitle();

        tvTotalExercise = findViewById(R.id.tv_total_exercise);
        tvGoal = findViewById(R.id.tv_goal);
        etInput = findViewById(R.id.et_exercise_input);
        spinnerExerciseType = findViewById(R.id.spinner_exercise_type);
        lvTodayRecords = findViewById(R.id.lv_today_records);
        btnConfirm = findViewById(R.id.btn_confirm_add);
        
        btnAdd1 = findViewById(R.id.btn_add_1);
        btnAdd10 = findViewById(R.id.btn_add_10);
        btnAdd30 = findViewById(R.id.btn_add_30);

        apiService = SupabaseClient.getApi(this);

        setupSpinnerWithLongClick();

        adapter = new ExerciseAdapter(this, logDataList, new ExerciseAdapter.OnRecordActionListener() {
            @Override
            public void onEdit(ExerciseLog log) {
                loadRecordForEdit(log);
            }
            @Override
            public void onDelete(ExerciseLog log) {
                showDeleteConfirmDialog(log);
            }
        });
        lvTodayRecords.setAdapter(adapter);

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
            startActivity(new Intent(ExerciseActivity.this, ExerciseTargetActivity.class));
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
        fetchExerciseTypes(); // 운동 종류 목록 갱신 + 정렬 + 자동 선택
        fetchTodayRecords();  // 기록 목록 갱신
        updateGoalUI();       
    }

    // [수정] 목표 텍스트에 10,000보 추가
    private void updateGoalUI() {
        SharedPreferences prefs = getSharedPreferences("HealthPrefs", Context.MODE_PRIVATE);
        int targetMinutes = prefs.getInt("exercise_target", 30); 
        
        String text = String.format(Locale.KOREA, "목표: %d분 또는 10,000보 이상 ⓘ", targetMinutes);
        tvGoal.setText(text);
    }

    // [추가] 선택된 종류에 따라 UI(버튼 및 힌트) 동적 업데이트
    private void updateUIForSelectedType(String type) {
        if ("걸음수".equals(type)) {
            btnAdd1.setText("+10보");
            btnAdd10.setText("+100보");
            btnAdd30.setText("+500보");
            btnAdd1.setOnClickListener(v -> addTime(10));
            btnAdd10.setOnClickListener(v -> addTime(100));
            btnAdd30.setOnClickListener(v -> addTime(500));
            etInput.setHint("걸음수 입력");
        } else {
            btnAdd1.setText("+1분");
            btnAdd10.setText("+10분");
            btnAdd30.setText("+30분");
            btnAdd1.setOnClickListener(v -> addTime(1));
            btnAdd10.setOnClickListener(v -> addTime(10));
            btnAdd30.setOnClickListener(v -> addTime(30));
            etInput.setHint("시간(분) 입력");
        }
    }

    private void setupSpinnerWithLongClick() {
        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, exerciseTypeList);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerExerciseType.setAdapter(spinnerAdapter);

        spinnerExerciseType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!exerciseTypeList.isEmpty()) {
                    selectedType = exerciseTypeList.get(position).getName();
                    updateUIForSelectedType(selectedType); // 선택 시 UI 즉각 업데이트
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 관리 화면으로 이동
        spinnerExerciseType.setOnLongClickListener(v -> {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(100);
                }
            }
            Intent intent = new Intent(ExerciseActivity.this, ExerciseTypeActivity.class);
            exerciseTypeLauncher.launch(intent);
            return true;
        });
    }

    private void fetchExerciseTypes() {
		if (!checkNetworkAndProceed()) {
            return;
        }
        apiService.getExerciseTypes().enqueue(new Callback<List<ExerciseType>>() {
            @Override
            public void onResponse(Call<List<ExerciseType>> call, Response<List<ExerciseType>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    exerciseTypeList.clear();
                    List<ExerciseType> fetched = response.body();

                    // 최신순 정렬
                    Collections.sort(fetched, new Comparator<ExerciseType>() {
                        @Override
                        public int compare(ExerciseType o1, ExerciseType o2) {
                            return Long.compare(o2.getId(), o1.getId());
                        }
                    });

                    exerciseTypeList.addAll(fetched);
                    spinnerAdapter.notifyDataSetChanged();

                    // 자동 선택 처리
                    if (pendingExerciseSelection != null) {
                        for (int i = 0; i < exerciseTypeList.size(); i++) {
                            if (exerciseTypeList.get(i).getName().equals(pendingExerciseSelection)) {
                                spinnerExerciseType.setSelection(i);
                                selectedType = pendingExerciseSelection;
                                break;
                            }
                        }
                        pendingExerciseSelection = null;
                    } else if (!exerciseTypeList.isEmpty()) {
                        boolean found = false;
                        for (int i = 0; i < exerciseTypeList.size(); i++) {
                            if (exerciseTypeList.get(i).getName().equals(selectedType)) {
                                spinnerExerciseType.setSelection(i);
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            selectedType = exerciseTypeList.get(0).getName();
                            spinnerExerciseType.setSelection(0);
                        }
                    } else {
                        selectedType = "";
                    }
                    updateUIForSelectedType(selectedType); // 목록 로드 후 UI 확실히 업데이트
                }
            }
            @Override
            public void onFailure(Call<List<ExerciseType>> call, Throwable t) {}
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

    private void loadRecordForEdit(ExerciseLog log) {
        editingLogId = log.getId();
        etInput.setText(String.valueOf(log.getMinutes()));
        for (int i = 0; i < exerciseTypeList.size(); i++) {
            if (exerciseTypeList.get(i).getName().equals(log.getExerciseType())) {
                spinnerExerciseType.setSelection(i);
                break;
            }
        }
        btnConfirm.setText("수정");
    }

    private void resetUI() {
        etInput.setText("");
        editingLogId = null;
        btnConfirm.setText("추가");
        if (spinnerExerciseType.getCount() > 0) spinnerExerciseType.setSelection(0);
    }

    private void saveRecordToServer(int minutes) {
		if (!checkNetworkAndProceed()) { 
            return;
        }
       ExerciseLog newLog = new ExerciseLog(targetDate, selectedType, minutes, currentUserId);
        
        apiService.insertExercise(newLog).enqueue(new Callback<Void>() {
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
        updateFields.put("exercise_type", selectedType);

        apiService.updateExercise("eq." + id, updateFields).enqueue(new Callback<Void>() {
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
        apiService.getTodayExerciseLogs(query).enqueue(new Callback<List<ExerciseLog>>() {
            @Override
            public void onResponse(Call<List<ExerciseLog>> call, Response<List<ExerciseLog>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    logDataList.clear();
                    int totalMinutes = 0;
                    int totalSteps = 0;
                    
                    for (ExerciseLog log : response.body()) {
                        if ("걸음수".equals(log.getExerciseType())) {
                            totalSteps += log.getMinutes();
                        } else {
                            totalMinutes += log.getMinutes();
                        }
                        logDataList.add(log);
                    }
                    
                    // 총합 표시는 일반 운동과 걸음수를 같이 보여줄 수 있게 처리 (예: "30분 / 10,000보")
                    if (totalSteps > 0) {
                        tvTotalExercise.setText(String.format("%,d분 / %,d보", totalMinutes, totalSteps));
                    } else {
                        tvTotalExercise.setText(String.format("%,d", totalMinutes));
                    }
                    
                    adapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onFailure(Call<List<ExerciseLog>> call, Throwable t) {}
        });
    }

    private void showDeleteConfirmDialog(ExerciseLog log) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_delete_confirm, null);
        builder.setView(view);
        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        TextView tvMessage = view.findViewById(R.id.tv_dialog_message);
        view.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btn_delete).setOnClickListener(v -> {
            apiService.deleteExercise("eq." + log.getId()).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) fetchTodayRecords();
                }
                @Override
                public void onFailure(Call<Void> call, Throwable t) {}
            });
            dialog.dismiss();
        });
        tvMessage.setText(log.getExerciseType() + " 삭제?");
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

    // [Adapter] 초록색(Green) 테마 적용 및 '걸음수' 예외처리
    private static class ExerciseAdapter extends ArrayAdapter<ExerciseLog> {
        private Context context;
        private List<ExerciseLog> logs;
        private OnRecordActionListener listener;

        public interface OnRecordActionListener {
            void onEdit(ExerciseLog log);
            void onDelete(ExerciseLog log);
        }

        public ExerciseAdapter(@NonNull Context context, List<ExerciseLog> logs, OnRecordActionListener listener) {
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

            ExerciseLog log = logs.get(position);

            TextView tvName = convertView.findViewById(R.id.tv_food_name);
            TextView tvAmount = convertView.findViewById(R.id.tv_protein_amt);
            TextView tvUnit = convertView.findViewById(R.id.tv_unit);
            View colorBar = convertView.findViewById(R.id.v_color_bar);

            int greenColor = Color.parseColor("#4CAF50"); // 초록색

            if (colorBar != null) colorBar.setBackgroundColor(greenColor);
            
            if (tvAmount != null) {
                tvAmount.setText(String.format("%,d", log.getMinutes()));
                tvAmount.setTextColor(greenColor);
            }
            if (tvUnit != null) {
                // [수정] 걸음수면 "보", 아니면 "분"
                if ("걸음수".equals(log.getExerciseType())) {
                    tvUnit.setText("보");
                } else {
                    tvUnit.setText("분");
                }
                tvUnit.setTextColor(greenColor);
            }
            if (tvName != null) {
                tvName.setText(log.getExerciseType());
            }

            View btnEdit = convertView.findViewById(R.id.iv_edit);
            View btnDelete = convertView.findViewById(R.id.iv_delete);

            if (btnEdit != null) btnEdit.setOnClickListener(v -> listener.onEdit(log));
            if (btnDelete != null) btnDelete.setOnClickListener(v -> listener.onDelete(log));

            return convertView;
        }
    }
}
