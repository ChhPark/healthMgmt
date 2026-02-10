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

public class NoAlcoholActivity extends BaseHealthActivity {

    private TextView tvTotalAlcohol;
    private EditText etInput;
    private Spinner spinnerAlcoholType;
    private ListView lvTodayRecords;
    private Button btnConfirm;

    private SupabaseApi apiService;
    private List<AlcoholLog> logDataList = new ArrayList<>();
    private AlcoholAdapter adapter;

    private String selectedAlcohol = "";
    private Long editingLogId = null;
	private String targetDate;

    // [수정] AlcoholType 사용
    private List<AlcoholType> alcoholTypeList = new ArrayList<>();
    private ArrayAdapter<AlcoholType> spinnerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_no_alcohol);
		
		initializeUserId(); // ✅ 추가
		
		targetDate = getTargetDateFromIntent(); // ("target_date");
        if (targetDate == null) {
            targetDate = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(new Date());
        }

        // [수정] 2. 헤더 텍스트 갱신 함수 호출
        updateHeaderTitle();

        tvTotalAlcohol = findViewById(R.id.tv_total_alcohol);
        etInput = findViewById(R.id.et_alcohol_input);
        spinnerAlcoholType = findViewById(R.id.spinner_alcohol_type);
        lvTodayRecords = findViewById(R.id.lv_today_records);
        btnConfirm = findViewById(R.id.btn_confirm_add);

        apiService = SupabaseClient.getApi(this);

        setupSpinnerWithLongClick();

        adapter = new AlcoholAdapter(this, logDataList, new AlcoholAdapter.OnRecordActionListener() {
            @Override
            public void onEdit(AlcoholLog log) {
                loadRecordForEdit(log);
            }
            @Override
            public void onDelete(AlcoholLog log) {
                showDeleteConfirmDialog(log);
            }
        });
        lvTodayRecords.setAdapter(adapter);

        // [수정] 버튼 단위 변경: 10, 100, 500
        findViewById(R.id.btn_add_10).setOnClickListener(v -> addAmountToInput(10));
        findViewById(R.id.btn_add_100).setOnClickListener(v -> addAmountToInput(100));
        findViewById(R.id.btn_add_500).setOnClickListener(v -> addAmountToInput(500));
        findViewById(R.id.btn_reset).setOnClickListener(v -> resetUI());
        btnConfirm.setOnClickListener(v -> handleConfirmClick());
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
        fetchAlcoholTypes(); // [중요] 술 종류 갱신
        fetchTodayRecords();
    }

    private void setupSpinnerWithLongClick() {
        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, alcoholTypeList);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAlcoholType.setAdapter(spinnerAdapter);

        spinnerAlcoholType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!alcoholTypeList.isEmpty()) selectedAlcohol = alcoholTypeList.get(position).getName();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // [핵심] 길게 누르면 "술 관리" 화면으로 이동
        spinnerAlcoholType.setOnLongClickListener(v -> {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(100);
                }
            }
            startActivity(new Intent(NoAlcoholActivity.this, AlcoholTypeActivity.class));
            return true;
        });
    }

    private void fetchAlcoholTypes() {
		if (!checkNetworkAndProceed()) { // ✅ 추가
        return;
    }
        apiService.getAlcoholTypes().enqueue(new Callback<List<AlcoholType>>() {
            @Override
            public void onResponse(Call<List<AlcoholType>> call, Response<List<AlcoholType>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    alcoholTypeList.clear();
                    alcoholTypeList.addAll(response.body());
                    spinnerAdapter.notifyDataSetChanged();
                    
                    if (!alcoholTypeList.isEmpty()) {
                        boolean found = false;
                        for (int i = 0; i < alcoholTypeList.size(); i++) {
                            if (alcoholTypeList.get(i).getName().equals(selectedAlcohol)) {
                                spinnerAlcoholType.setSelection(i);
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            selectedAlcohol = alcoholTypeList.get(0).getName();
                            spinnerAlcoholType.setSelection(0);
                        }
                    } else {
                        selectedAlcohol = "";
                    }
                }
            }
            @Override
            public void onFailure(Call<List<AlcoholType>> call, Throwable t) {}
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

    private void loadRecordForEdit(AlcoholLog log) {
        editingLogId = log.getId();
        etInput.setText(String.format("%,d", log.getAmount()));
        for (int i = 0; i < alcoholTypeList.size(); i++) {
            if (alcoholTypeList.get(i).getName().equals(log.getAlcoholType())) {
                spinnerAlcoholType.setSelection(i);
                break;
            }
        }
        btnConfirm.setText("수정");
    }

    private void resetUI() {
        etInput.setText("");
        editingLogId = null;
        btnConfirm.setText("추가");
        if (spinnerAlcoholType.getCount() > 0) spinnerAlcoholType.setSelection(0);
    }

    private void saveRecordToServer(int amount) {
		if (!checkNetworkAndProceed()) { // ✅ 추가
        return;
    }
        AlcoholLog newLog = new AlcoholLog(targetDate, selectedAlcohol, amount, currentUserId);
        apiService.insertAlcohol(newLog).enqueue(new Callback<Void>() {
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

    private void updateRecordToServer(long id, int newAmount) {
        Map<String, Object> updateFields = new HashMap<>();
        updateFields.put("amount", newAmount);
        updateFields.put("alcohol_type", selectedAlcohol);

        apiService.updateAlcohol("eq." + id, updateFields).enqueue(new Callback<Void>() {
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
        apiService.getTodayAlcoholLogs(query).enqueue(new Callback<List<AlcoholLog>>() {
            @Override
            public void onResponse(Call<List<AlcoholLog>> call, Response<List<AlcoholLog>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    logDataList.clear();
                    int totalSum = 0;
                    for (AlcoholLog log : response.body()) {
                        totalSum += log.getAmount();
                        logDataList.add(log);
                    }
                    tvTotalAlcohol.setText(String.format("%,d", totalSum));
                    adapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onFailure(Call<List<AlcoholLog>> call, Throwable t) {}
        });
    }

    private void showDeleteConfirmDialog(AlcoholLog log) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_delete_confirm, null);
        builder.setView(view);
        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        TextView tvMessage = view.findViewById(R.id.tv_dialog_message);
        view.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btn_delete).setOnClickListener(v -> {
            apiService.deleteAlcohol("eq." + log.getId()).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) fetchTodayRecords();
                }
                @Override
                public void onFailure(Call<Void> call, Throwable t) {}
            });
            dialog.dismiss();
        });
        tvMessage.setText(log.getAlcoholType() + " 삭제?");
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

    private static class AlcoholAdapter extends ArrayAdapter<AlcoholLog> {
        private Context context;
        private List<AlcoholLog> logs;
        private OnRecordActionListener listener;

        public interface OnRecordActionListener {
            void onEdit(AlcoholLog log);
            void onDelete(AlcoholLog log);
        }

        public AlcoholAdapter(@NonNull Context context, List<AlcoholLog> logs, OnRecordActionListener listener) {
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

            AlcoholLog log = logs.get(position);

            TextView tvName = convertView.findViewById(R.id.tv_food_name);
            TextView tvAmount = convertView.findViewById(R.id.tv_protein_amt);
            TextView tvUnit = convertView.findViewById(R.id.tv_unit);
            View colorBar = convertView.findViewById(R.id.v_color_bar);

            int purpleColor = Color.parseColor("#9C27B0");

            if (colorBar != null) colorBar.setBackgroundColor(purpleColor);
            
            if (tvAmount != null) {
                tvAmount.setText(String.format("%,d", log.getAmount()));
                tvAmount.setTextColor(purpleColor);
            }
            if (tvUnit != null) {
                tvUnit.setText("cc");
                tvUnit.setTextColor(purpleColor);
            }
            if (tvName != null) {
                tvName.setText(log.getAlcoholType());
            }

            View btnEdit = convertView.findViewById(R.id.iv_edit);
            View btnDelete = convertView.findViewById(R.id.iv_delete);

            if (btnEdit != null) btnEdit.setOnClickListener(v -> listener.onEdit(log));
            if (btnDelete != null) btnDelete.setOnClickListener(v -> listener.onDelete(log));

            return convertView;
        }
    }
}
