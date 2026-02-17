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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

public class WaterActivity extends BaseHealthActivity {

    private TextView tvTotalWater;
    private TextView tvGoal;
    private EditText etInput;
    private Spinner spinnerWaterType;
    private ListView lvTodayRecords;
    private Button btnConfirm;

    private List<WaterLog> logDataList = new ArrayList<>();
    private WaterAdapter adapter;

    private String selectedWater = "";
    private Long editingLogId = null;

    private List<WaterType> waterTypeList = new ArrayList<>();
    private ArrayAdapter<WaterType> spinnerAdapter;
    
    private boolean isOzMode = false;
    private static final double OZ_TO_CC = 29.5735;
    private TextView btnToggleUnit;
    private Button btnAdd10;
    private Button btnAdd100;
    private Button btnAdd500;
    
    private static final SimpleDateFormat DATE_FORMAT = 
        new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
    private static final SimpleDateFormat DATE_FORMAT_DISPLAY = 
        new SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_water);
		
		initializeUserId();
        
        targetDate = getTargetDateFromIntent();
        apiService = SupabaseClient.getApi(this);

        updateHeaderTitle();

        tvTotalWater = findViewById(R.id.tv_total_water);
        tvGoal = findViewById(R.id.tv_goal);
        etInput = findViewById(R.id.et_water_input);
        spinnerWaterType = findViewById(R.id.spinner_water_type);
        lvTodayRecords = findViewById(R.id.lv_today_records);
        btnConfirm = findViewById(R.id.btn_confirm_add);
        
        btnToggleUnit = findViewById(R.id.btn_toggle_unit);
        btnAdd10 = findViewById(R.id.btn_add_10);
        btnAdd100 = findViewById(R.id.btn_add_100);
        btnAdd500 = findViewById(R.id.btn_add_500);

        setupSpinnerWithLongClick();

        setupGoalClickListeners();

        adapter = new WaterAdapter(this, logDataList, new WaterAdapter.OnRecordActionListener() {
            @Override
            public void onEdit(WaterLog log) {
                loadRecordForEdit(log);
            }
            @Override
            public void onDelete(WaterLog log) {
                showDeleteConfirmDialog(log);
            }
        });
        lvTodayRecords.setAdapter(adapter);

        btnToggleUnit.setOnClickListener(v -> toggleUnit());
        
        btnAdd10.setOnClickListener(v -> addAmountToInput(isOzMode ? 1 : 10));
        btnAdd100.setOnClickListener(v -> addAmountToInput(isOzMode ? 5 : 100));
        btnAdd500.setOnClickListener(v -> addAmountToInput(isOzMode ? 10 : 500));
        findViewById(R.id.btn_reset).setOnClickListener(v -> resetUI());
        btnConfirm.setOnClickListener(v -> handleConfirmClick());
    }
    
    private void toggleUnit() {
        isOzMode = !isOzMode;
        updateUnitButtons();
        etInput.setText("");
    }
    
    private void updateUnitButtons() {
        if (isOzMode) {
            btnToggleUnit.setText("oz");
            btnAdd10.setText("+1oz");
            btnAdd100.setText("+5oz");
            btnAdd500.setText("+10oz");
            etInput.setHint("oz 입력");
        } else {
            btnToggleUnit.setText("cc");
            btnAdd10.setText("+10cc");
            btnAdd100.setText("+100cc");
            btnAdd500.setText("+500cc");
            etInput.setHint("cc 입력");
        }
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
        startActivity(new Intent(WaterActivity.this, WaterTargetActivity.class));
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
        updateGoalText();
        fetchWaterTypes();
        fetchTodayRecords();
    }

    private void updateGoalText() {
        SharedPreferences prefs = getSharedPreferences("HealthPrefs", Context.MODE_PRIVATE);
        int target = prefs.getInt("water_target", 2000);
		tvGoal.setText(String.format(Locale.KOREA, "목표: %,dcc 이상 ⓘ", target));
    }

    private void setupSpinnerWithLongClick() {
        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, waterTypeList);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerWaterType.setAdapter(spinnerAdapter);

        spinnerWaterType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!waterTypeList.isEmpty()) selectedWater = waterTypeList.get(position).getName();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerWaterType.setOnLongClickListener(v -> {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(100);
                }
            }
            startActivity(new Intent(WaterActivity.this, WaterTypeActivity.class));
            return true;
        });
    }

    private void fetchWaterTypes() {
		if (!checkNetworkAndProceed()) {
        return;
    }
        apiService.getWaterTypes().enqueue(new Callback<List<WaterType>>() {
            @Override
            public void onResponse(Call<List<WaterType>> call, Response<List<WaterType>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    waterTypeList.clear();
                    waterTypeList.addAll(response.body());
                    spinnerAdapter.notifyDataSetChanged();

                    if (!waterTypeList.isEmpty()) {
                        boolean found = false;
                        for (int i = 0; i < waterTypeList.size(); i++) {
                            if (waterTypeList.get(i).getName().equals(selectedWater)) {
                                spinnerWaterType.setSelection(i);
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            selectedWater = waterTypeList.get(0).getName();
                            spinnerWaterType.setSelection(0);
                        }
                    } else {
                        selectedWater = "";
                    }
                }
            }
            @Override
            public void onFailure(Call<List<WaterType>> call, Throwable t) {
                handleApiFailure(t);
            }
        });
    }

    private void handleConfirmClick() {
        String inputStr = etInput.getText().toString();
        if (inputStr.isEmpty()) return;

        try {
            int inputValue = Integer.parseInt(inputStr.replace(",", ""));
            int amountInCc = isOzMode ? (int)Math.round(inputValue * OZ_TO_CC) : inputValue;
            
            if (editingLogId == null) saveRecordToServer(amountInCc);
            else updateRecordToServer(editingLogId, amountInCc);
        } catch (NumberFormatException e) {
            showError("숫자만 입력 가능합니다.");
        }
    }

    private void loadRecordForEdit(WaterLog log) {
        editingLogId = log.getId();
        etInput.setText(String.format("%,d", log.getWaterAmount()));
        
        for (int i = 0; i < waterTypeList.size(); i++) {
            if (waterTypeList.get(i).getName().equals(log.getWaterType())) {
                spinnerWaterType.setSelection(i);
                break;
            }
        }
        btnConfirm.setText("수정");
    }

    private void resetUI() {
        etInput.setText("");
        editingLogId = null;
        btnConfirm.setText("추가");
        if (spinnerWaterType.getCount() > 0) spinnerWaterType.setSelection(0);
    }

    private void saveRecordToServer(int amount) {
		if (!checkNetworkAndProceed()) {
        return;
    }
        WaterLog log = new WaterLog(targetDate, selectedWater, amount, currentUserId);
        apiService.insertWater(log).enqueue(new Callback<Void>() {
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
        updateFields.put("water_amount", newAmount);
        updateFields.put("water_type", selectedWater);

        apiService.updateWater("eq." + id, updateFields).enqueue(new Callback<Void>() {
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
        String query = "eq." + targetDate;
        apiService.getTodayWaterLogs(query).enqueue(new Callback<List<WaterLog>>() {
            @Override
            public void onResponse(Call<List<WaterLog>> call, Response<List<WaterLog>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    logDataList.clear();
                    int totalSum = 0;
                    for (WaterLog log : response.body()) {
                        totalSum += log.getWaterAmount();
                        logDataList.add(log);
                    }
                    tvTotalWater.setText(String.format("%,d", totalSum));
                    adapter.notifyDataSetChanged();
                } else {
                    showError("기록을 불러올 수 없습니다.");
                }
            }
            @Override
            public void onFailure(Call<List<WaterLog>> call, Throwable t) {
                handleApiFailure(t);
            }
        });
    }

    private void showDeleteConfirmDialog(WaterLog log) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_delete_confirm, null);
        builder.setView(view);
        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        TextView tvMessage = view.findViewById(R.id.tv_dialog_message);
        view.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btn_delete).setOnClickListener(v -> {
            apiService.deleteWater("eq." + log.getId()).enqueue(new Callback<Void>() {
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
        tvMessage.setText(log.getWaterType() + " 삭제?");
        dialog.show();
    }

    private void addAmountToInput(int amount) {
        String currentText = etInput.getText().toString();
        currentText = currentText.replace(",", "");
        
        int currentVal = 0;
        try { 
            if (!currentText.isEmpty()) {
                currentVal = Integer.parseInt(currentText); 
            }
        } catch (Exception e) {}
        
        int newVal = currentVal + amount;
        
        if (isOzMode) {
            etInput.setText(String.valueOf(newVal));
        } else {
            etInput.setText(String.format("%,d", newVal));
        }
    }

    private static class WaterAdapter extends ArrayAdapter<WaterLog> {
        private Context context;
        private List<WaterLog> logs;
        private OnRecordActionListener listener;
        private static final double OZ_TO_CC = 29.5735;

        public interface OnRecordActionListener {
            void onEdit(WaterLog log);
            void onDelete(WaterLog log);
        }

        public WaterAdapter(@NonNull Context context, List<WaterLog> logs, OnRecordActionListener listener) {
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

            WaterLog log = logs.get(position);

            TextView tvName = convertView.findViewById(R.id.tv_food_name);
            TextView tvAmount = convertView.findViewById(R.id.tv_protein_amt);
            TextView tvUnit = convertView.findViewById(R.id.tv_unit);
            View colorBar = convertView.findViewById(R.id.v_color_bar);

            int blueColor = Color.parseColor("#2196F3");

            if (colorBar != null) colorBar.setBackgroundColor(blueColor);
            
            if (tvAmount != null) {
                int cc = log.getWaterAmount();
                int oz = (int)Math.round(cc / OZ_TO_CC);
                
                if (oz > 0) {
                    tvAmount.setText(String.format("%,dcc (%doz)", cc, oz));
                } else {
                    tvAmount.setText(String.format("%,d", cc));
                }
                tvAmount.setTextColor(blueColor);
            }
            if (tvUnit != null) {
                tvUnit.setText("");
                tvUnit.setTextColor(blueColor);
            }
            if (tvName != null) {
                tvName.setText(log.getWaterType());
            }

            View btnEdit = convertView.findViewById(R.id.iv_edit);
            View btnDelete = convertView.findViewById(R.id.iv_delete);

            if (btnEdit != null) btnEdit.setOnClickListener(v -> listener.onEdit(log));
            if (btnDelete != null) btnDelete.setOnClickListener(v -> listener.onDelete(log));

            return convertView;
        }
    }
}