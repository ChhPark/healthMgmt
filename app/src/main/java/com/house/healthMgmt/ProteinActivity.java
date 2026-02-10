package com.house.healthMgmt;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;

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
import android.widget.Toast;

public class ProteinActivity extends BaseHealthActivity {

    private TextView tvTotalProtein;
    private TextView tvGoal;
    private EditText etInput;
    private Spinner spinnerFoodType;
    private ListView lvTodayRecords;
    private Button btnConfirm;

    private List<ProteinLog> logDataList = new ArrayList<>();
    private ProteinRecordAdapter recordAdapter;

    private String selectedFood = "";
    private Long editingLogId = null;

    private List<FoodType> foodTypeList = new ArrayList<>();
    private ArrayAdapter<FoodType> spinnerAdapter;
    
    private static final SimpleDateFormat DATE_FORMAT = 
        new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
    private static final SimpleDateFormat DATE_FORMAT_DISPLAY = 
        new SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_protein);
		
		initializeUserId(); // ✅ 추가
        
        targetDate = getTargetDateFromIntent();
        apiService = SupabaseClient.getApi(this);
        
        updateHeaderTitle();

        tvTotalProtein = findViewById(R.id.tv_total_protein);
        tvGoal = findViewById(R.id.tv_goal);
        etInput = findViewById(R.id.et_protein_input);
        spinnerFoodType = findViewById(R.id.spinner_food_type);
        lvTodayRecords = findViewById(R.id.lv_today_records);
        btnConfirm = findViewById(R.id.btn_confirm_add);

        setupSpinnerWithLongClick();

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

        findViewById(R.id.btn_add_1).setOnClickListener(v -> addAmountToInput(1));
        findViewById(R.id.btn_add_5).setOnClickListener(v -> addAmountToInput(5));
        findViewById(R.id.btn_add_10).setOnClickListener(v -> addAmountToInput(10));
        findViewById(R.id.btn_reset).setOnClickListener(v -> resetUI());
        btnConfirm.setOnClickListener(v -> handleConfirmClick());

        setupGoalClickListeners();
    }
	
	private void setupGoalClickListeners() {
    // ✅ 클릭 효과 활성화
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
            "💡 목표를 길게 누르면 체중 입력 화면으로 이동합니다", 
            Toast.LENGTH_SHORT).show();
    });
    
    tvGoal.setOnLongClickListener(v -> {
        Intent intent = new Intent(ProteinActivity.this, WeightActivity.class);
        startActivity(intent);
        return true;
    });
}

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        
        if (intent != null) {
            setIntent(intent); 
            targetDate = getTargetDateFromIntent();
            updateHeaderTitle();
            fetchTodayRecords();
        }
    }

    private void updateHeaderTitle() {
        TextView tvHeader = findViewById(R.id.tv_record_header);
        if (tvHeader != null) {
            String todayStr = DATE_FORMAT.format(new Date());

            if (targetDate.equals(todayStr)) {
                tvHeader.setText("오늘의 기록");
            } else {
                try {
                    Date date = DATE_FORMAT.parse(targetDate);
                    tvHeader.setText(DATE_FORMAT_DISPLAY.format(date) + "의 기록");
                } catch (Exception e) {
                    tvHeader.setText(targetDate + "의 기록");
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchFoodTypes();
        fetchTodayRecords();
        updateGoalFromWeight();
    }

    private void updateGoalFromWeight() {
        apiService.getLatestWeight().enqueue(new Callback<List<WeightLog>>() {
            @Override
            public void onResponse(Call<List<WeightLog>> call, Response<List<WeightLog>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    double weight = response.body().get(0).getWeight();
                    int goal = (int) Math.round(weight * 0.7);
                    tvGoal.setText("목표: " + goal + "g (" + weight + "kg) ⓘ");
                } else {
                    tvGoal.setText("목표: 설정 필요 ⓘ");
                }
            }
            @Override
            public void onFailure(Call<List<WeightLog>> call, Throwable t) {
                handleApiFailure(t);
                tvGoal.setText("목표: 설정 필요 ⓘ");
            }
        });
    }

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
		if (!checkNetworkAndProceed()) { // ✅ 추가
        return;
    }
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
            public void onFailure(Call<List<FoodType>> call, Throwable t) {
                handleApiFailure(t);
            }
        });
    }

    private void handleConfirmClick() {
        String inputStr = etInput.getText().toString();
        if (inputStr.isEmpty()) return;
        
        try {
            int amount = Integer.parseInt(inputStr);
            if (editingLogId == null) saveRecordToServer(amount);
            else updateRecordToServer(editingLogId, amount);
        } catch (NumberFormatException e) {
            showError("숫자만 입력 가능합니다.");
        }
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
		if (!checkNetworkAndProceed()) { // ✅ 추가
        return;
    }
        ProteinLog newLog = new ProteinLog(targetDate, selectedFood, amount, currentUserId);
        
        apiService.insertProtein(newLog).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    showSuccess("저장됨");
                    resetUI();
                    fetchTodayRecords();
                } else {
                    showError("저장 실패");
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                handleApiFailure(t);
            }
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
                    showSuccess("수정됨");
                    resetUI();
                    fetchTodayRecords();
                } else {
                    showError("수정 실패");
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                handleApiFailure(t);
            }
        });
    }

    private void fetchTodayRecords() {
        String queryDate = "eq." + targetDate;
        apiService.getTodayLogs(queryDate).enqueue(new Callback<List<ProteinLog>>() {
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
                } else {
                    showError("기록을 불러올 수 없습니다.");
                }
            }
            @Override
            public void onFailure(Call<List<ProteinLog>> call, Throwable t) {
                handleApiFailure(t);
            }
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
                    if (response.isSuccessful()) {
                        showSuccess("삭제됨");
                        fetchTodayRecords();
                    } else {
                        showError("삭제 실패");
                    }
                }
                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    handleApiFailure(t);
                }
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
