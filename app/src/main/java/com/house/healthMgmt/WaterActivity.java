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
import android.widget.ImageView;
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

public class WaterActivity extends AppCompatActivity {

    private TextView tvTotalWater;
    private TextView tvGoal;
    private EditText etInput;
    private Spinner spinnerWaterType;
    private ListView lvTodayRecords;
    private Button btnConfirm;

    private SupabaseApi apiService;
    private List<WaterLog> logDataList = new ArrayList<>();
    private WaterAdapter adapter;

    private String selectedWater = "";
    private String currentUserId = "user_01";
    private Long editingLogId = null;
	private String targetDate; 

    // [변경] 물 종류 관리용 리스트 (FoodType -> WaterType)
    private List<WaterType> waterTypeList = new ArrayList<>();
    private ArrayAdapter<WaterType> spinnerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_water);
		
		targetDate = getIntent().getStringExtra("target_date");
        if (targetDate == null) {
            targetDate = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(new Date());
        }

        // [수정] 2. 헤더 텍스트 설정 (함수 호출)
        updateHeaderTitle();

        tvTotalWater = findViewById(R.id.tv_total_water);
        tvGoal = findViewById(R.id.tv_goal);
        etInput = findViewById(R.id.et_water_input);
        spinnerWaterType = findViewById(R.id.spinner_water_type);
        lvTodayRecords = findViewById(R.id.lv_today_records);
        btnConfirm = findViewById(R.id.btn_confirm_add);

        apiService = SupabaseClient.getApi(this);

        setupSpinnerWithLongClick();

        // 목표 텍스트 클릭 시 목표 설정 화면으로 이동
        tvGoal.setOnClickListener(v -> {
            startActivity(new Intent(WaterActivity.this, WaterTargetActivity.class));
        });

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

        findViewById(R.id.btn_add_10).setOnClickListener(v -> addAmountToInput(10));
        findViewById(R.id.btn_add_100).setOnClickListener(v -> addAmountToInput(100));
        findViewById(R.id.btn_add_500).setOnClickListener(v -> addAmountToInput(500));
        findViewById(R.id.btn_reset).setOnClickListener(v -> resetUI());
        btnConfirm.setOnClickListener(v -> handleConfirmClick());
    }
	
	    // [추가] 액티비티가 재사용될 때 날짜 갱신
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

    // [추가] 헤더 텍스트 변경 로직 (오늘 vs 과거/미래)
    private void updateHeaderTitle() {
        TextView tvHeader = findViewById(R.id.tv_record_header);
        if (tvHeader != null) {
            String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(new Date());

            if (targetDate.equals(todayStr)) {
                tvHeader.setText("오늘의 기록");
            } else {
                try {
                    // 날짜 포맷 변경 (yyyy-MM-dd -> yyyy년 MM월 dd일)
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
        updateGoalText();   // 목표값 갱신
        fetchWaterTypes();  // 물 종류 목록 갱신 (관리 화면에서 돌아올 때 반영)
        fetchTodayRecords();
    }

    // 저장된 목표값 불러오기
    private void updateGoalText() {
        SharedPreferences prefs = getSharedPreferences("HealthPrefs", Context.MODE_PRIVATE);
        int target = prefs.getInt("water_target", 2000); // 기본값 2000
        tvGoal.setText(String.format(Locale.KOREA, "목표: %,dcc 이상", target));
    }

    // 스피너 설정 및 롱 클릭 리스너 (물 관리 화면 이동)
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

        // [핵심] 길게 누르면 "물 종류 관리" 화면으로 이동
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

    // 물 종류 목록 서버에서 가져오기
    private void fetchWaterTypes() {
        apiService.getWaterTypes().enqueue(new Callback<List<WaterType>>() {
            @Override
            public void onResponse(Call<List<WaterType>> call, Response<List<WaterType>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    waterTypeList.clear();
                    waterTypeList.addAll(response.body());
                    spinnerAdapter.notifyDataSetChanged();

                    // 목록이 있으면 적절한 항목 선택 (기존 선택 유지 또는 첫 번째)
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
            public void onFailure(Call<List<WaterType>> call, Throwable t) {}
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
            Toast.makeText(this, "숫자만 입력 가능합니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadRecordForEdit(WaterLog log) {
        editingLogId = log.getId();
        etInput.setText(String.format("%,d", log.getWaterAmount()));
        
        // 스피너 선택
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
        WaterLog log = new WaterLog(targetDate, selectedWater, amount, currentUserId);
        apiService.insertWater(log).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(WaterActivity.this, "저장됨", Toast.LENGTH_SHORT).show();
                    resetUI();
                    fetchTodayRecords();
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(WaterActivity.this, "저장 실패", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(WaterActivity.this, "수정됨", Toast.LENGTH_SHORT).show();
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
                }
            }
            @Override
            public void onFailure(Call<List<WaterLog>> call, Throwable t) {}
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
                    if (response.isSuccessful()) fetchTodayRecords();
                }
                @Override
                public void onFailure(Call<Void> call, Throwable t) {}
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
        etInput.setText(String.format("%,d", newVal));
    }

    // [어댑터] 파란색 테마 및 cc 단위 적용
    private static class WaterAdapter extends ArrayAdapter<WaterLog> {
        private Context context;
        private List<WaterLog> logs;
        private OnRecordActionListener listener;

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
                // 단백질/나트륨과 동일한 레이아웃 사용 (색상만 변경)
                convertView = LayoutInflater.from(context).inflate(R.layout.item_protein_record, parent, false);
            }

            WaterLog log = logs.get(position);

            TextView tvName = convertView.findViewById(R.id.tv_food_name);
            TextView tvAmount = convertView.findViewById(R.id.tv_protein_amt);
            TextView tvUnit = convertView.findViewById(R.id.tv_unit);
            View colorBar = convertView.findViewById(R.id.v_color_bar);

            int blueColor = Color.parseColor("#2196F3"); // 파란색

            if (colorBar != null) colorBar.setBackgroundColor(blueColor);
            
            if (tvAmount != null) {
                tvAmount.setText(String.format("%,d", log.getWaterAmount()));
                tvAmount.setTextColor(blueColor);
            }
            if (tvUnit != null) {
                tvUnit.setText("cc"); // 단위 변경
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
