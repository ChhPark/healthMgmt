package com.house.healthMgmt;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
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
    private EditText etWeight;
    private ListView lvHistory;
    private SupabaseApi apiService;
    
    // 리스트 관련
    private List<WeightLog> weightList = new ArrayList<>();
    private WeightAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weight);

        etWeight = findViewById(R.id.et_weight);
        lvHistory = findViewById(R.id.lv_weight_history);
        apiService = SupabaseClient.getApi(this);

        // 어댑터 연결
        adapter = new WeightAdapter(this, weightList);
        lvHistory.setAdapter(adapter);

        findViewById(R.id.btn_save_weight).setOnClickListener(v -> saveWeight());
        
        // 초기 데이터 로드
        fetchWeightHistory();
    }

    private void saveWeight() {
        String weightStr = etWeight.getText().toString();
        if (weightStr.isEmpty()) {
            Toast.makeText(this, "체중을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        double weight = Double.parseDouble(weightStr);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(new Date());

        WeightLog log = new WeightLog(today, weight, "user_01");

        apiService.insertWeight(log).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(WeightActivity.this, "저장되었습니다.", Toast.LENGTH_SHORT).show();
                    etWeight.setText(""); // 입력창 비우기
                    fetchWeightHistory(); // [중요] 리스트 갱신
                    // finish(); // 제거됨: 저장 후에도 화면 유지하며 리스트 확인
                } else {
                    Toast.makeText(WeightActivity.this, "저장 실패", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(WeightActivity.this, "오류 발생", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 서버에서 체중 기록 목록 가져오기
    private void fetchWeightHistory() {
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
}
