package com.house.healthMgmt;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private TextView tvDateTitle;
    private TextView tvProteinValue; // [수정] 원형 텍스트뷰 연결
    private SupabaseApi apiService;
    private String todayDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI 연결
        tvDateTitle = findViewById(R.id.tv_date_title);
        tvProteinValue = findViewById(R.id.tv_protein_value); // 단백질 카드 안의 원형 텍스트

        apiService = SupabaseClient.getApi(this);

        // 오늘 날짜 구하기
        todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(new Date());

        // 1. 헤더 날짜 업데이트
        updateDateHeader();

        // 2. 단백질 카드 클릭 리스너
        findViewById(R.id.card_protein).setOnClickListener(v -> {
            startActivity(new Intent(this, ProteinActivity.class));
        });

        // (다른 카드 클릭 리스너는 필요 시 추가)
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 화면이 다시 보일 때마다 목표 달성 여부 체크
        checkProteinGoal();
    }

    private void updateDateHeader() {
        String displayDate = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA).format(new Date());
        tvDateTitle.setText(displayDate);
    }

    // 단백질 목표 달성 체크 로직
    private void checkProteinGoal() {
        // 1. 최신 체중 가져오기
        apiService.getLatestWeight().enqueue(new Callback<List<WeightLog>>() {
            @Override
            public void onResponse(Call<List<WeightLog>> call, Response<List<WeightLog>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    double weight = response.body().get(0).getWeight();
                    calculateProteinStatus(weight);
                } else {
                    // 체중 기록 없으면 판단 불가 (-)
                    tvProteinValue.setText("-");
                }
            }
            @Override
            public void onFailure(Call<List<WeightLog>> call, Throwable t) {}
        });
    }

    private void calculateProteinStatus(double weight) {
        // 목표: 체중 * 0.7 (반올림)
        int goalLimit = (int) Math.round(weight * 0.7);

        // 2. 오늘 먹은 단백질 총량 가져오기
        String dateQuery = "eq." + todayDate;
        apiService.getTodayLogs(dateQuery).enqueue(new Callback<List<ProteinLog>>() {
            @Override
            public void onResponse(Call<List<ProteinLog>> call, Response<List<ProteinLog>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int totalProtein = 0;
                    for (ProteinLog log : response.body()) {
                        totalProtein += log.getProteinAmount();
                    }

                    // 3. 비교 (목표 '이하' 섭취 시 성공)
                    boolean isSuccess = totalProtein <= goalLimit;

                    // 4. UI 업데이트 (원 안에 O 또는 X 표시)
                    tvProteinValue.setText(isSuccess ? "O" : "X");

                    // 5. DB 저장
                    saveDailyResult(isSuccess);
                }
            }
            @Override
            public void onFailure(Call<List<ProteinLog>> call, Throwable t) {}
        });
    }

    private void saveDailyResult(boolean proteinSuccess) {
        DailySummary summary = new DailySummary(todayDate, "user_01", proteinSuccess);
        
        apiService.upsertDailySummary(summary).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {}
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("MainActivity", "Save Error", t);
            }
        });
    }
}
