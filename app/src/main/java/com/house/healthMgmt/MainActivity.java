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
    private TextView tvProteinValue; // 단백질 O/X
    private TextView tvSodiumValue;  // 나트륨 O/X
    
    private SupabaseApi apiService;
    private String todayDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. UI 연결
        tvDateTitle = findViewById(R.id.tv_date_title);
        tvProteinValue = findViewById(R.id.tv_protein_value);
        tvSodiumValue = findViewById(R.id.tv_sodium_value); // XML에 이 ID가 있어야 함

        apiService = SupabaseClient.getApi(this);

        // 오늘 날짜 구하기 (yyyy-MM-dd)
        todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(new Date());

        // 2. 헤더 날짜 업데이트
        updateDateHeader();

        // 3. 클릭 리스너 설정 (화면 이동)
        
        // [단백질 카드] -> ProteinActivity
        findViewById(R.id.card_protein).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ProteinActivity.class));
        });

        // [나트륨 카드] -> SodiumActivity
        // (XML에서 나트륨 카드뷰에 android:id="@+id/card_sodium" 추가 필수)
        findViewById(R.id.card_sodium).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SodiumActivity.class));
        });

        // (물, 수면 등 다른 카드는 아직 구현 전이므로 토스트 메시지나 공란 처리)
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 화면이 다시 보일 때마다 데이터 갱신 (상세화면에서 수정하고 왔을 때 반영)
        checkProteinGoal();
        checkSodiumGoal();
    }

    private void updateDateHeader() {
        String displayDate = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA).format(new Date());
        tvDateTitle.setText(displayDate);
    }

    // --- [1. 단백질 목표 달성 체크 (체중 * 0.7)] ---
    private void checkProteinGoal() {
        // 1) 최신 체중 가져오기
        apiService.getLatestWeight().enqueue(new Callback<List<WeightLog>>() {
            @Override
            public void onResponse(Call<List<WeightLog>> call, Response<List<WeightLog>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    double weight = response.body().get(0).getWeight();
                    calculateProteinStatus(weight);
                } else {
                    tvProteinValue.setText("-"); // 체중 기록 없음
                }
            }
            @Override
            public void onFailure(Call<List<WeightLog>> call, Throwable t) {
                Log.e("MainActivity", "Weight Error", t);
            }
        });
    }

    private void calculateProteinStatus(double weight) {
        int goalLimit = (int) Math.round(weight * 0.7);

        // 2) 오늘 먹은 단백질 총량
        String dateQuery = "eq." + todayDate;
        apiService.getTodayLogs(dateQuery).enqueue(new Callback<List<ProteinLog>>() {
            @Override
            public void onResponse(Call<List<ProteinLog>> call, Response<List<ProteinLog>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int totalProtein = 0;
                    for (ProteinLog log : response.body()) {
                        totalProtein += log.getProteinAmount();
                    }

                    // 비교: 목표량 '이하'면 성공(O), 초과면 실패(X)
                    // (단백질은 보통 '이상' 섭취가 목표일 수도 있으나, 질문자님 로직인 '이하' 유지)
                    boolean isSuccess = totalProtein <= goalLimit; 
                    
                    tvProteinValue.setText(isSuccess ? "O" : "X");
                }
            }
            @Override
            public void onFailure(Call<List<ProteinLog>> call, Throwable t) {}
        });
    }

    // --- [2. 나트륨 목표 달성 체크 (2,000mg 이하)] ---
    private void checkSodiumGoal() {
        int sodiumGoal = 2000; // 목표: 2000mg

        String dateQuery = "eq." + todayDate;
        apiService.getTodaySodiumLogs(dateQuery).enqueue(new Callback<List<SodiumLog>>() {
            @Override
            public void onResponse(Call<List<SodiumLog>> call, Response<List<SodiumLog>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int totalSodium = 0;
                    for (SodiumLog log : response.body()) {
                        totalSodium += log.getSodiumAmount();
                    }

                    // 비교: 2000mg '이하'면 성공(O)
                    boolean isSuccess = totalSodium <= sodiumGoal;

                    tvSodiumValue.setText(isSuccess ? "O" : "X");
                }
            }
            @Override
            public void onFailure(Call<List<SodiumLog>> call, Throwable t) {
                Log.e("MainActivity", "Sodium Error", t);
            }
        });
    }
}
