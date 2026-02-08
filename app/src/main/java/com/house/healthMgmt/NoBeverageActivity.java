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

public class NoBeverageActivity extends AppCompatActivity {

    private TextView tvTotalBeverage;
    private EditText etInput;
    private Spinner spinnerBeverageType;
    private ListView lvTodayRecords;
    private Button btnConfirm;

    private SupabaseApi apiService;
    private List<BeverageLog> logDataList = new ArrayList<>();
    private BeverageAdapter adapter;

    private String selectedBeverage = "";
    private String currentUserId = "user_01";
    private Long editingLogId = null;

    private List<BeverageType> beverageTypeList = new ArrayList<>();
    private ArrayAdapter<BeverageType> spinnerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_no_beverage);

        tvTotalBeverage = findViewById(R.id.tv_total_beverage);
        etInput = findViewById(R.id.et_beverage_input);
        spinnerBeverageType = findViewById(R.id.spinner_beverage_type);
        lvTodayRecords = findViewById(R.id.lv_today_records);
        btnConfirm = findViewById(R.id.btn_confirm_add);

        apiService = SupabaseClient.getApi(this);

        setupSpinnerWithLongClick();

        adapter = new BeverageAdapter(this, logDataList, new BeverageAdapter.OnRecordActionListener() {
            @Override
            public void onEdit(BeverageLog log) {
                loadRecordForEdit(log);
            }
            @Override
            public void onDelete(BeverageLog log) {
                showDeleteConfirmDialog(log);
            }
        });
        lvTodayRecords.setAdapter(adapter);

        findViewById(R.id.btn_add_100).setOnClickListener(v -> addAmountToInput(100));
        findViewById(R.id.btn_add_500).setOnClickListener(v -> addAmountToInput(500));
        findViewById(R.id.btn_add_1000).setOnClickListener(v -> addAmountToInput(1000));
        findViewById(R.id.btn_reset).setOnClickListener(v -> resetUI());
        btnConfirm.setOnClickListener(v -> handleConfirmClick());
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchBeverageTypes();
        fetchTodayRecords();
    }

    private void setupSpinnerWithLongClick() {
        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, beverageTypeList);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBeverageType.setAdapter(spinnerAdapter);

        spinnerBeverageType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!beverageTypeList.isEmpty()) selectedBeverage = beverageTypeList.get(position).getName();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerBeverageType.setOnLongClickListener(v -> {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(100);
                }
            }
            startActivity(new Intent(NoBeverageActivity.this, BeverageTypeActivity.class));
            return true;
        });
    }

    private void fetchBeverageTypes() {
        apiService.getBeverageTypes().enqueue(new Callback<List<BeverageType>>() {
            @Override
            public void onResponse(Call<List<BeverageType>> call, Response<List<BeverageType>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    beverageTypeList.clear();
                    beverageTypeList.addAll(response.body());
                    spinnerAdapter.notifyDataSetChanged();

                    if (!beverageTypeList.isEmpty()) {
                        boolean found = false;
                        for (int i = 0; i < beverageTypeList.size(); i++) {
                            if (beverageTypeList.get(i).getName().equals(selectedBeverage)) {
                                spinnerBeverageType.setSelection(i);
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            selectedBeverage = beverageTypeList.get(0).getName();
                            spinnerBeverageType.setSelection(0);
                        }
                    } else {
                        selectedBeverage = "";
                    }
                }
            }
            @Override
            public void onFailure(Call<List<BeverageType>> call, Throwable t) {}
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

    private void loadRecordForEdit(BeverageLog log) {
        editingLogId = log.getId();
        etInput.setText(String.format("%,d", log.getAmount()));
        
        for (int i = 0; i < beverageTypeList.size(); i++) {
            if (beverageTypeList.get(i).getName().equals(log.getBeverageType())) {
                spinnerBeverageType.setSelection(i);
                break;
            }
        }
        btnConfirm.setText("수정");
    }

    private void resetUI() {
        etInput.setText("");
        editingLogId = null;
        btnConfirm.setText("추가");
        if (spinnerBeverageType.getCount() > 0) spinnerBeverageType.setSelection(0);
    }

    private void saveRecordToServer(int amount) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(new Date());
        BeverageLog newLog = new BeverageLog(today, selectedBeverage, amount, currentUserId);
        apiService.insertBeverage(newLog).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(NoBeverageActivity.this, "저장됨", Toast.LENGTH_SHORT).show();
                    resetUI();
                    fetchTodayRecords();
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(NoBeverageActivity.this, "저장 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateRecordToServer(long id, int newAmount) {
        Map<String, Object> updateFields = new HashMap<>();
        updateFields.put("amount", newAmount);
        updateFields.put("beverage_type", selectedBeverage);

        apiService.updateBeverage("eq." + id, updateFields).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(NoBeverageActivity.this, "수정됨", Toast.LENGTH_SHORT).show();
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
        apiService.getTodayBeverageLogs(today).enqueue(new Callback<List<BeverageLog>>() {
            @Override
            public void onResponse(Call<List<BeverageLog>> call, Response<List<BeverageLog>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    logDataList.clear();
                    int totalSum = 0;
                    
                    for (BeverageLog log : response.body()) {
                        // [수정] "디카페인 커피"는 합계에서 제외
                        if (!"디카페인 커피".equals(log.getBeverageType())) {
                            totalSum += log.getAmount();
                        }
                        logDataList.add(log);
                    }
                    tvTotalBeverage.setText(String.format("%,d", totalSum));
                    adapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onFailure(Call<List<BeverageLog>> call, Throwable t) {}
        });
    }

    private void showDeleteConfirmDialog(BeverageLog log) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_delete_confirm, null);
        builder.setView(view);
        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        TextView tvMessage = view.findViewById(R.id.tv_dialog_message);
        view.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btn_delete).setOnClickListener(v -> {
            apiService.deleteBeverage("eq." + log.getId()).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) fetchTodayRecords();
                }
                @Override
                public void onFailure(Call<Void> call, Throwable t) {}
            });
            dialog.dismiss();
        });
        tvMessage.setText(log.getBeverageType() + " 삭제?");
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

    private static class BeverageAdapter extends ArrayAdapter<BeverageLog> {
        private Context context;
        private List<BeverageLog> logs;
        private OnRecordActionListener listener;

        public interface OnRecordActionListener {
            void onEdit(BeverageLog log);
            void onDelete(BeverageLog log);
        }

        public BeverageAdapter(@NonNull Context context, List<BeverageLog> logs, OnRecordActionListener listener) {
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

            BeverageLog log = logs.get(position);

            TextView tvName = convertView.findViewById(R.id.tv_food_name);
            TextView tvAmount = convertView.findViewById(R.id.tv_protein_amt);
            TextView tvUnit = convertView.findViewById(R.id.tv_unit);
            View colorBar = convertView.findViewById(R.id.v_color_bar);

            int redColor = Color.parseColor("#F44336");

            if (colorBar != null) colorBar.setBackgroundColor(redColor);
            
            if (tvAmount != null) {
                tvAmount.setText(String.format("%,d", log.getAmount()));
                tvAmount.setTextColor(redColor);
            }
            if (tvUnit != null) {
                tvUnit.setText("cc");
                tvUnit.setTextColor(redColor);
            }
            if (tvName != null) {
                tvName.setText(log.getBeverageType());
            }

            View btnEdit = convertView.findViewById(R.id.iv_edit);
            View btnDelete = convertView.findViewById(R.id.iv_delete);

            if (btnEdit != null) btnEdit.setOnClickListener(v -> listener.onEdit(log));
            if (btnDelete != null) btnDelete.setOnClickListener(v -> listener.onDelete(log));

            return convertView;
        }
    }
}
