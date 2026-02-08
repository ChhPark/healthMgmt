package com.house.healthMgmt;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
    private TextView tvProteinValue;     // 단백질 O/X
    private TextView tvSodiumValue;      // 나트륨 O/X
    private TextView tvWaterValue;       // 물 O/X
    private TextView tvNoBeverageValue;  // No음료수 O/X (추가됨)
    
    private SupabaseApi apiService;
    private String todayDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. UI 연결
        tvDateTitle = findViewById(R.id.tv_date_title);
        tvProteinValue = findViewById(R.id.tv_protein_value);
        tvSodiumValue = findViewById(R.id.tv_sodium_value);
        tvWaterValue = findViewById(R.id.tv_water_value);
        tvNoBeverageValue = findViewById(R.id.tv_no_beverage_value); // XML ID 필요

        apiService = SupabaseClient.getApi(this);

        // 오늘 날짜 구하기 (yyyy-MM-dd)
        todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(new Date());

        // 2. 헤더 날짜 업데이트
        updateDateHeader();

        // 3. 클릭 리스너 설정 (화면 이동)
        
        // [단백질]
        findViewById(R.id.card_protein).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ProteinActivity.class));
        });

        // [나트륨]
        findViewById(R.id.card_sodium).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SodiumActivity.class));
        });

        // [물]
        findViewById(R.id.card_water).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, WaterActivity.class));
        });

        // [No음료수] (추가됨)
        findViewById(R.id.card_no_beverage).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, NoBeverageActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 화면이 다시 보일 때마다 모든 데이터 갱신
        checkProteinGoal();
        checkSodiumGoal();
        checkWaterGoal();
        checkBeverageGoal(); // 음료수 체크 추가
    }

    private void updateDateHeader() {
        String displayDate = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA).format(new Date());
        tvDateTitle.setText(displayDate);
    }

    // --- [1. 단백질 목표 달성 체크 (체중 * 0.7)] ---
    private void checkProteinGoal() {
        apiService.getLatestWeight().enqueue(new Callback<List<WeightLog>>() {
            @Override
            public void onResponse(Call<List<WeightLog>> call, Response<List<WeightLog>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    double weight = response.body().get(0).getWeight();
                    calculateProteinStatus(weight);
                } else {
                    tvProteinValue.setText("-");
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
        String dateQuery = "eq." + todayDate;

        apiService.getTodayLogs(dateQuery).enqueue(new Callback<List<ProteinLog>>() {
            @Override
            public void onResponse(Call<List<ProteinLog>> call, Response<List<ProteinLog>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int totalProtein = 0;
                    for (ProteinLog log : response.body()) {
                        totalProtein += log.getProteinAmount();
                    }
                    // 목표 '이하'면 성공
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
        int sodiumGoal = 2000;
        String dateQuery = "eq." + todayDate;

        apiService.getTodaySodiumLogs(dateQuery).enqueue(new Callback<List<SodiumLog>>() {
            @Override
            public void onResponse(Call<List<SodiumLog>> call, Response<List<SodiumLog>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int totalSodium = 0;
                    for (SodiumLog log : response.body()) {
                        totalSodium += log.getSodiumAmount();
                    }
                    // 2000mg '이하'면 성공
                    boolean isSuccess = totalSodium <= sodiumGoal;
                    tvSodiumValue.setText(isSuccess ? "O" : "X");
                }
            }
            @Override
            public void onFailure(Call<List<SodiumLog>> call, Throwable t) {}
        });
    }

    // --- [3. 물 목표 달성 체크 (설정값 이상)] ---
    private void checkWaterGoal() {
        SharedPreferences prefs = getSharedPreferences("HealthPrefs", Context.MODE_PRIVATE);
        int waterGoal = prefs.getInt("water_target", 2000); // 기본값 2000
        String dateQuery = "eq." + todayDate;

        apiService.getTodayWaterLogs(dateQuery).enqueue(new Callback<List<WaterLog>>() {
            @Override
            public void onResponse(Call<List<WaterLog>> call, Response<List<WaterLog>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int totalWater = 0;
                    for (WaterLog log : response.body()) {
                        totalWater += log.getWaterAmount();
                    }
                    // 목표량 '이상'이면 성공
                    boolean isSuccess = totalWater >= waterGoal;
                    tvWaterValue.setText(isSuccess ? "O" : "X");
                }
            }
            @Override
            public void onFailure(Call<List<WaterLog>> call, Throwable t) {}
        });
    }

    // --- [4. No음료수 목표 달성 체크 (0cc 유지)] ---
    private void checkBeverageGoal() {
        // 목표: 0cc (마시면 안 됨)
        String dateQuery = "eq." + todayDate;

        apiService.getTodayBeverageLogs(dateQuery).enqueue(new Callback<List<BeverageLog>>() {
            @Override
            public void onResponse(Call<List<BeverageLog>> call, Response<List<BeverageLog>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int totalBeverage = 0;
                    for (BeverageLog log : response.body()) {
                        totalBeverage += log.getAmount();
                    }
                    
                    // 하나도 안 마셨으면(0) 성공(O), 마셨으면(>0) 실패(X)
                    boolean isSuccess = totalBeverage == 0;
                    tvNoBeverageValue.setText(isSuccess ? "O" : "X");
                }
            }
            @Override
            public void onFailure(Call<List<BeverageLog>> call, Throwable t) {}
        });
    }
}
