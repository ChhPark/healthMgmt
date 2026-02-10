package com.house.healthMgmt;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WeightActivity extends AppCompatActivity {

    private EditText etWeightInput;
    private Button btnSave;
    private ListView lvWeightHistory;

    private SupabaseApi apiService;
    private List<WeightLog> weightList = new ArrayList<>();
    private WeightAdapter adapter;
    
    private static final SimpleDateFormat DATE_FORMAT = 
        new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weight);

        etWeightInput = findViewById(R.id.et_weight_input);
        btnSave = findViewById(R.id.btn_save_weight);
        lvWeightHistory = findViewById(R.id.lv_weight_history);

        apiService = SupabaseClient.getApi(this);

        adapter = new WeightAdapter(this, weightList);
        lvWeightHistory.setAdapter(adapter);

        btnSave.setOnClickListener(v -> saveWeight());
        
        lvWeightHistory.setOnItemLongClickListener((parent, view, position, id) -> {
            WeightLog log = weightList.get(position);
            showDeleteDialog(log);
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchWeightLogs();
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showSuccess(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void saveWeight() {
        String input = etWeightInput.getText().toString();
        if (input.isEmpty()) {
            showError("체중을 입력하세요.");
            return;
        }

        try {
            double weight = Double.parseDouble(input);
            String today = DATE_FORMAT.format(new Date());

            WeightLog log = new WeightLog(today, weight, "user_01");

            apiService.insertWeight(log).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        showSuccess("저장되었습니다.");
                        etWeightInput.setText("");
                        
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.hideSoftInputFromWindow(etWeightInput.getWindowToken(), 0);
                        }
                        
                        fetchWeightLogs();
                    } else {
                        showError("저장 실패");
                    }
                }
                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    android.util.Log.e("WeightActivity", "API Error", t);
                    showError("저장 중 오류가 발생했습니다.");
                }
            });
        } catch (NumberFormatException e) {
            showError("올바른 숫자를 입력하세요.");
        }
    }

    private void fetchWeightLogs() {
        apiService.getWeightLogs().enqueue(new Callback<List<WeightLog>>() {
            @Override
            public void onResponse(Call<List<WeightLog>> call, Response<List<WeightLog>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    weightList.clear();
                    weightList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                } else {
                    showError("기록을 불러올 수 없습니다.");
                }
            }
            @Override
            public void onFailure(Call<List<WeightLog>> call, Throwable t) {
                android.util.Log.e("WeightActivity", "API Error", t);
                showError("데이터 로딩 중 오류가 발생했습니다.");
            }
        });
    }

    private void showDeleteDialog(WeightLog log) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("삭제 확인")
                .setMessage("이 기록을 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> {
                    showError("삭제 기능은 준비중입니다.");
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private class WeightAdapter extends ArrayAdapter<WeightLog> {
        private Context context;
        private List<WeightLog> list;

        public WeightAdapter(Context context, List<WeightLog> list) {
            super(context, 0, list);
            this.context = context;
            this.list = list;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_2, parent, false);
            }

            WeightLog log = list.get(position);

            TextView text1 = convertView.findViewById(android.R.id.text1);
            TextView text2 = convertView.findViewById(android.R.id.text2);

            text1.setText(log.getWeight() + " kg");
            text1.setTextSize(18);
            text1.setTextColor(Color.parseColor("#009688"));

            text2.setText(log.getRecordDate());
            text2.setTextColor(Color.GRAY);

            return convertView;
        }
    }
}
