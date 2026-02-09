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

public class ExerciseActivity extends AppCompatActivity {

    private TextView tvTotalExercise;
    private TextView tvGoal;
    private EditText etInput;
    private Spinner spinnerExerciseType;
    private ListView lvTodayRecords;
    private Button btnConfirm;

    private SupabaseApi apiService;
    private List<ExerciseLog> logDataList = new ArrayList<>();
    private ExerciseAdapter adapter;

    private String selectedType = "";
    private String currentUserId = "user_01";
    private Long editingLogId = null;
	private String targetDate;

    // 운동 종류 관리용 리스트
    private List<ExerciseType> exerciseTypeList = new ArrayList<>();
    private ArrayAdapter<ExerciseType> spinnerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise);
		
		targetDate = getIntent().getStringExtra("target_date");
        if (targetDate == null) {
            targetDate = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(new Date());
        }

        // [수정] 2. 헤더 텍스트 갱신 함수 호출
        updateHeaderTitle();

        tvTotalExercise = findViewById(R.id.tv_total_exercise);
        tvGoal = findViewById(R.id.tv_goal);
        etInput = findViewById(R.id.et_exercise_input);
        spinnerExerciseType = findViewById(R.id.spinner_exercise_type);
        lvTodayRecords = findViewById(R.id.lv_today_records);
        btnConfirm = findViewById(R.id.btn_confirm_add);

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

        // 버튼: +1분, +10분, +30분
        findViewById(R.id.btn_add_1).setOnClickListener(v -> addTime(1));
        findViewById(R.id.btn_add_10).setOnClickListener(v -> addTime(10));
        findViewById(R.id.btn_add_30).setOnClickListener(v -> addTime(30));
        
        findViewById(R.id.btn_reset).setOnClickListener(v -> resetUI());
        btnConfirm.setOnClickListener(v -> handleConfirmClick());

        // [수정] 운동 목표 설정 화면으로 이동
        tvGoal.setOnClickListener(v -> {
            startActivity(new Intent(ExerciseActivity.this, ExerciseTargetActivity.class));
        });
    }
	
	    // [추가] 액티비티 재사용 시 날짜 갱신
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        targetDate = intent.getStringExtra("target_date");
        if (targetDate == null) {
            targetDate = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(new Date());
        }

        updateHeaderTitle(); // 헤더 갱신
        fetchTodayRecords(); // 데이터 다시 조회
    }

    // [추가] 헤더 텍스트 변경 로직 (오늘 vs yyyy년 mm월 dd일)
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
        fetchExerciseTypes(); // 운동 종류 목록 갱신
        fetchTodayRecords();  // 기록 목록 갱신
        updateGoalUI();       // 목표 텍스트 갱신 (설정화면에서 돌아왔을 때 반영)
    }

    private void updateGoalUI() {
        SharedPreferences prefs = getSharedPreferences("HealthPrefs", Context.MODE_PRIVATE);
        int targetMinutes = prefs.getInt("exercise_target", 30); // 기본값 30분
        
        // 예: "목표: 30분 이상"
        String text = String.format(Locale.KOREA, "목표: %d분 이상", targetMinutes);
        tvGoal.setText(text);
    }

    private void setupSpinnerWithLongClick() {
        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, exerciseTypeList);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerExerciseType.setAdapter(spinnerAdapter);

        spinnerExerciseType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!exerciseTypeList.isEmpty()) selectedType = exerciseTypeList.get(position).getName();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // [핵심] 길게 누르면 "운동 종류 관리" 화면으로 이동
        spinnerExerciseType.setOnLongClickListener(v -> {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(100);
                }
            }
            startActivity(new Intent(ExerciseActivity.this, ExerciseTypeActivity.class));
            return true;
        });
    }

    private void fetchExerciseTypes() {
        apiService.getExerciseTypes().enqueue(new Callback<List<ExerciseType>>() {
            @Override
            public void onResponse(Call<List<ExerciseType>> call, Response<List<ExerciseType>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    exerciseTypeList.clear();
                    exerciseTypeList.addAll(response.body());
                    spinnerAdapter.notifyDataSetChanged();

                    // 목록이 있으면 적절한 항목 선택
                    if (!exerciseTypeList.isEmpty()) {
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
       ExerciseLog newLog = new ExerciseLog(targetDate, selectedType, minutes, currentUserId);
        
        apiService.insertExercise(newLog).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ExerciseActivity.this, "저장됨", Toast.LENGTH_SHORT).show();
                    resetUI();
                    fetchTodayRecords();
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(ExerciseActivity.this, "저장 실패", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(ExerciseActivity.this, "수정됨", Toast.LENGTH_SHORT).show();
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
                    for (ExerciseLog log : response.body()) {
                        totalMinutes += log.getMinutes();
                        logDataList.add(log);
                    }
                    tvTotalExercise.setText(String.format("%,d", totalMinutes));
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

    // [Adapter] 초록색(Green) 테마 적용
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
                // 레이아웃 파일은 기존과 공유하거나 복사해서 사용 (item_protein_record 등)
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
                tvAmount.setText(String.valueOf(log.getMinutes()));
                tvAmount.setTextColor(greenColor);
            }
            if (tvUnit != null) {
                tvUnit.setText("분");
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
