package com.house.healthMgmt;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
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

public class ProteinActivity extends AppCompatActivity {

    private TextView tvTotalProtein;
    private TextView tvGoal; // [추가] 목표 표시 텍스트뷰
    private EditText etInput;
    private Spinner spinnerFoodType;
    private ListView lvTodayRecords;
    private Button btnConfirm;

    private SupabaseApi apiService;
    private List<ProteinLog> logDataList = new ArrayList<>();
    private ProteinRecordAdapter recordAdapter;

    private String selectedFood = "";
    private String currentUserId = "user_01";
    private Long editingLogId = null;

    private List<FoodType> foodTypeList = new ArrayList<>();
    private ArrayAdapter<FoodType> spinnerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_protein);

        // 1. UI 연결
        tvTotalProtein = findViewById(R.id.tv_total_protein);
        tvGoal = findViewById(R.id.tv_goal); // [중요] 목표 텍스트뷰 연결
        etInput = findViewById(R.id.et_protein_input);
        spinnerFoodType = findViewById(R.id.spinner_food_type);
        lvTodayRecords = findViewById(R.id.lv_today_records);
        btnConfirm = findViewById(R.id.btn_confirm_add);

        // 2. API 초기화
        apiService = SupabaseClient.getApi(this);

        // 3. 스피너 설정
        setupSpinnerWithLongClick();

        // 4. 리스트 어댑터 설정
        recordAdapter = new ProteinRecordAdapter(this, logDataList, new ProteinRecordAdapter.OnRecordActionListener() {
            @Override
            public void onEdit(ProteinLog log) {
                loadRecordForEdit(log);
            }
            @Override
            public void onDelete(ProteinLog log) {
                showDeleteConfirmDialog(log);
            }
        });
        lvTodayRecords.setAdapter(recordAdapter);

        // 5. 버튼 이벤트
        findViewById(R.id.btn_add_1).setOnClickListener(v -> addAmountToInput(1));
        findViewById(R.id.btn_add_5).setOnClickListener(v -> addAmountToInput(5));
        findViewById(R.id.btn_add_10).setOnClickListener(v -> addAmountToInput(10));
        findViewById(R.id.btn_reset).setOnClickListener(v -> resetUI());
        btnConfirm.setOnClickListener(v -> handleConfirmClick());

        // [추가] 목표 텍스트 클릭 시 체중 화면으로 이동
        tvGoal.setOnClickListener(v -> {
            Intent intent = new Intent(ProteinActivity.this, WeightActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 화면이 다시 보일 때마다 데이터 갱신
        fetchFoodTypes();
        fetchTodayRecords();
        updateGoalFromWeight(); // [핵심] 체중 기반 목표 업데이트
    }

    // --- [기능: 체중 가져와서 목표 계산] ---
    private void updateGoalFromWeight() {
        // 가장 최근 체중 1개를 가져옴
        apiService.getLatestWeight().enqueue(new Callback<List<WeightLog>>() {
            @Override
            public void onResponse(Call<List<WeightLog>> call, Response<List<WeightLog>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    // 최신 체중 가져오기
                    double weight = response.body().get(0).getWeight();
                    // 목표 계산 (체중 * 0.7) 반올림
                    int goal = (int) Math.round(weight * 0.7);
                    
                    // UI 업데이트
                    tvGoal.setText("목표: " + goal + "g (" + weight + "kg)");
                } else {
                    tvGoal.setText("목표: 설정 필요 (클릭)");
                }
            }

            @Override
            public void onFailure(Call<List<WeightLog>> call, Throwable t) {
                // 에러 시 조용히 무시하거나 로그 출력
                Log.e("ProteinActivity", "Weight fetch failed", t);
            }
        });
    }

    // --- [기존 로직들 유지] ---

    private void setupSpinnerWithLongClick() {
        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, foodTypeList);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFoodType.setAdapter(spinnerAdapter);

        spinnerFoodType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!foodTypeList.isEmpty()) selectedFood = foodTypeList.get(position).getName();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerFoodType.setOnLongClickListener(v -> {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(100);
                }
            }
            Intent intent = new Intent(ProteinActivity.this, FoodActivity.class);
            startActivity(intent);
            return true;
        });
    }

    private void fetchFoodTypes() {
        apiService.getFoodTypes().enqueue(new Callback<List<FoodType>>() {
            @Override
            public void onResponse(Call<List<FoodType>> call, Response<List<FoodType>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    foodTypeList.clear();
                    foodTypeList.addAll(response.body());
                    spinnerAdapter.notifyDataSetChanged();
                    if (!foodTypeList.isEmpty()) selectedFood = foodTypeList.get(0).getName();
                }
            }
            @Override
            public void onFailure(Call<List<FoodType>> call, Throwable t) {}
        });
    }

    private void handleConfirmClick() {
        String inputStr = etInput.getText().toString();
        if (inputStr.isEmpty()) return;
        int amount = Integer.parseInt(inputStr);

        if (editingLogId == null) saveRecordToServer(amount);
        else updateRecordToServer(editingLogId, amount);
    }

    private void loadRecordForEdit(ProteinLog log) {
        editingLogId = log.getId();
        etInput.setText(String.valueOf(log.getProteinAmount()));
        for (int i = 0; i < foodTypeList.size(); i++) {
            if (foodTypeList.get(i).getName().equals(log.getFoodType())) {
                spinnerFoodType.setSelection(i);
                break;
            }
        }
        btnConfirm.setText("수정");
    }

    private void resetUI() {
        etInput.setText("");
        editingLogId = null;
        btnConfirm.setText("추가");
        if (spinnerFoodType.getCount() > 0) spinnerFoodType.setSelection(0);
    }

    private void saveRecordToServer(int amount) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(new Date());
        ProteinLog newLog = new ProteinLog(today, selectedFood, amount, currentUserId);
        apiService.insertProtein(newLog).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ProteinActivity.this, "저장됨", Toast.LENGTH_SHORT).show();
                    resetUI();
                    fetchTodayRecords();
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {}
        });
    }

    private void updateRecordToServer(long id, int newAmount) {
        Map<String, Object> updateFields = new HashMap<>();
        updateFields.put("protein_amount", newAmount);
        updateFields.put("food_type", selectedFood);

        apiService.updateProtein("eq." + id, updateFields).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ProteinActivity.this, "수정됨", Toast.LENGTH_SHORT).show();
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
        apiService.getTodayLogs(today).enqueue(new Callback<List<ProteinLog>>() {
            @Override
            public void onResponse(Call<List<ProteinLog>> call, Response<List<ProteinLog>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    logDataList.clear();
                    int totalSum = 0;
                    for (ProteinLog log : response.body()) {
                        totalSum += log.getProteinAmount();
                        logDataList.add(log);
                    }
                    tvTotalProtein.setText(String.valueOf(totalSum));
                    recordAdapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onFailure(Call<List<ProteinLog>> call, Throwable t) {}
        });
    }

    private void showDeleteConfirmDialog(ProteinLog log) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_delete_confirm, null);
        builder.setView(view);
        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        TextView tvMessage = view.findViewById(R.id.tv_dialog_message);
        view.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btn_delete).setOnClickListener(v -> {
            apiService.deleteProtein("eq." + log.getId()).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) fetchTodayRecords();
                }
                @Override
                public void onFailure(Call<Void> call, Throwable t) {}
            });
            dialog.dismiss();
        });
        tvMessage.setText(log.getFoodType() + " " + log.getProteinAmount() + "g 삭제?");
        dialog.show();
    }

    private void addAmountToInput(int amount) {
        String currentText = etInput.getText().toString();
        int currentVal = 0;
        try { currentVal = Integer.parseInt(currentText); } catch (Exception e) {}
        etInput.setText(String.valueOf(currentVal + amount));
    }
}
