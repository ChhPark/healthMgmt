package com.house.healthMgmt;

import android.content.Context;
import android.content.Intent;
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

public class SodiumActivity extends BaseHealthActivity {

    private TextView tvTotalSodium;
    private EditText etInput;
    private Spinner spinnerFoodType;
    private ListView lvTodayRecords;
    private Button btnConfirm;

    private List<SodiumLog> logDataList = new ArrayList<>();
    private SodiumAdapter adapter;

    private String selectedFood = "";
    private Long editingLogId = null;

    // 선택 화면에서 돌아왔을 때, 선택해야 할 음식을 잠시 기억하는 변수
    private String pendingFoodSelection = null;

    private List<FoodType> foodTypeList = new ArrayList<>();
    private ArrayAdapter<FoodType> spinnerAdapter;

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
    private static final SimpleDateFormat DATE_FORMAT_DISPLAY =
            new SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA);

    // 음식 관리 화면 결과 처리 런처
    private final ActivityResultLauncher<Intent> foodActivityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String selectedName = result.getData().getStringExtra("selected_food");
                    if (selectedName != null) {
                        this.pendingFoodSelection = selectedName;
                        this.selectedFood = selectedName;
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sodium);

        initializeUserId();

        targetDate = getTargetDateFromIntent();
        apiService = SupabaseClient.getApi(this);

        updateHeaderTitle();

        tvTotalSodium = findViewById(R.id.tv_total_sodium);
        etInput = findViewById(R.id.et_sodium_input);
        spinnerFoodType = findViewById(R.id.spinner_food_type);
        lvTodayRecords = findViewById(R.id.lv_today_records);
        btnConfirm = findViewById(R.id.btn_confirm_add);

        setupSpinnerWithLongClick();

        // 어댑터 설정
        adapter = new SodiumAdapter(this, logDataList, new SodiumAdapter.OnRecordActionListener() {
            @Override
            public void onEdit(SodiumLog log) {
                loadRecordForEdit(log);
            }

            @Override
            public void onDelete(SodiumLog log) {
                showDeleteConfirmDialog(log);
            }
        });
        lvTodayRecords.setAdapter(adapter);

        findViewById(R.id.btn_add_10).setOnClickListener(v -> addAmountToInput(10));
        findViewById(R.id.btn_add_100).setOnClickListener(v -> addAmountToInput(100));
        findViewById(R.id.btn_add_500).setOnClickListener(v -> addAmountToInput(500));
        findViewById(R.id.btn_reset).setOnClickListener(v -> resetUI());
        btnConfirm.setOnClickListener(v -> handleConfirmClick());
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
        fetchFoodTypes(); // 목록 갱신 + 자동 선택 + 정렬 수행
        fetchTodayRecords();
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
            public void onNothingSelected(AdapterView<?> parent) {
            }
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
            Intent intent = new Intent(SodiumActivity.this, FoodActivity.class);
            foodActivityLauncher.launch(intent);
            return true;
        });
    }

    private void fetchFoodTypes() {
        if (!checkNetworkAndProceed()) {
            return;
        }
        apiService.getFoodTypes().enqueue(new Callback<List<FoodType>>() {
            @Override
            public void onResponse(Call<List<FoodType>> call, Response<List<FoodType>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    foodTypeList.clear();
                    
                    List<FoodType> fetchedList = response.body();

                    // ID 기준 내림차순 정렬 (최신순)
                    Collections.sort(fetchedList, new Comparator<FoodType>() {
                        @Override
                        public int compare(FoodType o1, FoodType o2) {
                            return Long.compare(o2.getId(), o1.getId());
                        }
                    });

                    foodTypeList.addAll(fetchedList);
                    spinnerAdapter.notifyDataSetChanged();

                    // 선택 화면에서 받아온 값이 있으면 자동 선택
                    if (pendingFoodSelection != null) {
                        for (int i = 0; i < foodTypeList.size(); i++) {
                            if (foodTypeList.get(i).getName().equals(pendingFoodSelection)) {
                                spinnerFoodType.setSelection(i);
                                selectedFood = pendingFoodSelection;
                                break;
                            }
                        }
                        pendingFoodSelection = null;
                    } else if (!foodTypeList.isEmpty()) {
                        boolean currentSelectionExists = false;
                        for (int i = 0; i < foodTypeList.size(); i++) {
                            if (foodTypeList.get(i).getName().equals(selectedFood)) {
                                spinnerFoodType.setSelection(i);
                                currentSelectionExists = true;
                                break;
                            }
                        }
                        if (!currentSelectionExists) {
                            spinnerFoodType.setSelection(0);
                            selectedFood = foodTypeList.get(0).getName();
                        }
                    }
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
            int amount = Integer.parseInt(inputStr.replace(",", ""));
            if (editingLogId == null) saveRecordToServer(amount);
            else updateRecordToServer(editingLogId, amount);
        } catch (NumberFormatException e) {
            showError("숫자만 입력 가능합니다.");
        }
    }

    private void loadRecordForEdit(SodiumLog log) {
        editingLogId = log.getId();
        etInput.setText(String.format("%,d", log.getSodiumAmount()));
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
        if (!checkNetworkAndProceed()) {
            return;
        }
        SodiumLog newLog = new SodiumLog(targetDate, selectedFood, amount, currentUserId);

        apiService.insertSodium(newLog).enqueue(new Callback<Void>() {
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
        updateFields.put("sodium_amount", newAmount);
        updateFields.put("food_type", selectedFood);

        apiService.updateSodium("eq." + id, updateFields).enqueue(new Callback<Void>() {
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
        apiService.getTodaySodiumLogs(query).enqueue(new Callback<List<SodiumLog>>() {
            @Override
            public void onResponse(Call<List<SodiumLog>> call, Response<List<SodiumLog>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    logDataList.clear();
                    int totalSum = 0;
                    for (SodiumLog log : response.body()) {
                        totalSum += log.getSodiumAmount();
                        logDataList.add(log);
                    }
                    tvTotalSodium.setText(String.format("%,d", totalSum));
                    adapter.notifyDataSetChanged();
                } else {
                    showError("기록을 불러올 수 없습니다.");
                }
            }

            @Override
            public void onFailure(Call<List<SodiumLog>> call, Throwable t) {
                handleApiFailure(t);
            }
        });
    }

    private void showDeleteConfirmDialog(SodiumLog log) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_delete_confirm, null);
        builder.setView(view);
        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        TextView tvMessage = view.findViewById(R.id.tv_dialog_message);
        view.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btn_delete).setOnClickListener(v -> {
            apiService.deleteSodium("eq." + log.getId()).enqueue(new Callback<Void>() {
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
        tvMessage.setText(log.getFoodType() + " 삭제?");
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
        } catch (Exception e) {
        }

        int newVal = currentVal + amount;
        etInput.setText(String.format("%,d", newVal));
    }

    // [중요] 누락되었던 Adapter 클래스 정의
    private static class SodiumAdapter extends ArrayAdapter<SodiumLog> {
        private Context context;
        private List<SodiumLog> logs;
        private OnRecordActionListener listener;

        public interface OnRecordActionListener {
            void onEdit(SodiumLog log);
            void onDelete(SodiumLog log);
        }

        public SodiumAdapter(@NonNull Context context, List<SodiumLog> logs, OnRecordActionListener listener) {
            super(context, 0, logs);
            this.context = context;
            this.logs = logs;
            this.listener = listener;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                // 단백질과 동일한 레이아웃 재사용 (없으면 item_sodium_record 등을 만들어야 함)
                convertView = LayoutInflater.from(context).inflate(R.layout.item_protein_record, parent, false);
            }

            SodiumLog log = logs.get(position);

            TextView tvFoodName = convertView.findViewById(R.id.tv_food_name);
            TextView tvAmount = convertView.findViewById(R.id.tv_protein_amt);
            TextView tvUnit = convertView.findViewById(R.id.tv_unit);
            View colorBar = convertView.findViewById(R.id.v_color_bar);

            // 나트륨 색상 (#FFB300)
            int orangeColor = Color.parseColor("#FFB300");

            if (colorBar != null) colorBar.setBackgroundColor(orangeColor);

            if (tvAmount != null) {
                tvAmount.setText(String.format("%,d", log.getSodiumAmount()));
                tvAmount.setTextColor(orangeColor);
            }
            if (tvUnit != null) {
                tvUnit.setText("mg");
                tvUnit.setTextColor(orangeColor);
            }
            if (tvFoodName != null) {
                tvFoodName.setText(log.getFoodType());
            }

            View btnEdit = convertView.findViewById(R.id.iv_edit);
            View btnDelete = convertView.findViewById(R.id.iv_delete);

            if (btnEdit != null) btnEdit.setOnClickListener(v -> listener.onEdit(log));
            if (btnDelete != null) btnDelete.setOnClickListener(v -> listener.onDelete(log));

            return convertView;
        }
    }
}
