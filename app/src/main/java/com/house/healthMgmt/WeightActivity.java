package com.house.healthMgmt;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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

    private EditText etWeightInput; // 변수명 변경
    private Button btnSave;
    private ListView lvWeightHistory; // 변수명 변경

    private SupabaseApi apiService;
    private List<WeightLog> weightList = new ArrayList<>();
    private WeightAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weight);

        // 1. UI 연결 (새로운 XML ID 사용)
        etWeightInput = findViewById(R.id.et_weight_input); // et_weight -> et_weight_input
        btnSave = findViewById(R.id.btn_save_weight);
        lvWeightHistory = findViewById(R.id.lv_weight_history); // lv_history -> lv_weight_history

        apiService = SupabaseClient.getApi(this);

        // 2. 리스트 어댑터 설정
        adapter = new WeightAdapter(this, weightList);
        lvWeightHistory.setAdapter(adapter);

        // 3. 저장 버튼 클릭
        btnSave.setOnClickListener(v -> saveWeight());
        
        // 4. 리스트 아이템 길게 눌러 삭제
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

    private void saveWeight() {
        String input = etWeightInput.getText().toString();
        if (input.isEmpty()) return;

        try {
            double weight = Double.parseDouble(input);
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(new Date());

            WeightLog log = new WeightLog(today, weight, "user_01");

            apiService.insertWeight(log).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(WeightActivity.this, "저장되었습니다.", Toast.LENGTH_SHORT).show();
                        etWeightInput.setText("");
                        // 키보드 내리기
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(etWeightInput.getWindowToken(), 0);
                        
                        fetchWeightLogs();
                    }
                }
                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Toast.makeText(WeightActivity.this, "저장 실패", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (NumberFormatException e) {
            Toast.makeText(this, "올바른 숫자를 입력하세요.", Toast.LENGTH_SHORT).show();
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
                }
            }
            @Override
            public void onFailure(Call<List<WeightLog>> call, Throwable t) {}
        });
    }

    private void showDeleteDialog(WeightLog log) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("삭제 확인")
                .setMessage("이 기록을 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> {
                    // 삭제 API 호출 (ID가 없으면 구현 불가할 수 있음. 테이블에 ID 컬럼 권장)
                    // 현재는 편의상 UI에서만 지우거나, API가 있다면 호출
                    // apiService.deleteWeight(...); 
                    Toast.makeText(this, "기능 준비중입니다.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("취소", null)
                .show();
    }

    // 내부 클래스: 체중 리스트 어댑터
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
                // 단순한 기본 레이아웃 사용 (커스텀 필요 시 item_weight.xml 생성)
                convertView = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_2, parent, false);
            }

            WeightLog log = list.get(position);

            TextView text1 = convertView.findViewById(android.R.id.text1);
            TextView text2 = convertView.findViewById(android.R.id.text2);

            text1.setText(log.getWeight() + " kg");
            text1.setTextSize(18);
            text1.setTextColor(Color.parseColor("#009688")); // Teal 색상

            text2.setText(log.getRecordDate());
            text2.setTextColor(Color.GRAY);

            return convertView;
        }
    }
}
