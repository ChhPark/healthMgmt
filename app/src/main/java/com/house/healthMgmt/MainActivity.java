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
    private TextView tvNoBeverageValue;  // No음료수 O/X
    private TextView tvNoAlcoholValue;   // No술 O/X
    private TextView tvSleepValue;       // 수면 O/X
    
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
        tvNoBeverageValue = findViewById(R.id.tv_no_beverage_value);
        tvNoAlcoholValue = findViewById(R.id.tv_no_alcohol_value);
        tvSleepValue = findViewById(R.id.tv_sleep_value);

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

        // [No음료수]
        findViewById(R.id.card_no_beverage).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, NoBeverageActivity.class));
        });

        // [No술]
        findViewById(R.id.card_no_alcohol).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, NoAlcoholActivity.class));
        });

        // [수면]
        findViewById(R.id.card_sleep).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SleepActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 화면이 다시 보일 때마다 모든 데이터 갱신
        checkProteinGoal();
        checkSodiumGoal();
        checkWaterGoal();
        checkBeverageGoal();
        checkAlcoholGoal();
        checkSleepGoal(); 
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
        String dateQuery = "eq." + todayDate;
        apiService.getTodayBeverageLogs(dateQuery).enqueue(new Callback<List<BeverageLog>>() {
            @Override
            public void onResponse(Call<List<BeverageLog>> call, Response<List<BeverageLog>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int totalBeverage = 0;
                    for (BeverageLog log : response.body()) {
                        if (!"디카페인 커피".equals(log.getBeverageType())) {
                            totalBeverage += log.getAmount();
                        }
                    }
                    boolean isSuccess = totalBeverage == 0;
                    tvNoBeverageValue.setText(isSuccess ? "O" : "X");
                }
            }
            @Override
            public void onFailure(Call<List<BeverageLog>> call, Throwable t) {}
        });
    }

    // --- [5. No술 목표 달성 체크 (0cc 유지)] ---
    private void checkAlcoholGoal() {
        String dateQuery = "eq." + todayDate;
        apiService.getTodayAlcoholLogs(dateQuery).enqueue(new Callback<List<AlcoholLog>>() {
            @Override
            public void onResponse(Call<List<AlcoholLog>> call, Response<List<AlcoholLog>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int totalAlcohol = 0;
                    for (AlcoholLog log : response.body()) {
                        totalAlcohol += log.getAmount();
                    }
                    boolean isSuccess = totalAlcohol == 0;
                    tvNoAlcoholValue.setText(isSuccess ? "O" : "X");
                }
            }
            @Override
            public void onFailure(Call<List<AlcoholLog>> call, Throwable t) {}
        });
    }

    // --- [6. 수면 목표 달성 체크 (설정값 이상)] ---
    private void checkSleepGoal() {
        // [수정] 저장된 목표값 가져오기 (기본값 420)
        SharedPreferences prefs = getSharedPreferences("HealthPrefs", Context.MODE_PRIVATE);
        int sleepGoal = prefs.getInt("sleep_target", 420); 

        String dateQuery = "eq." + todayDate;

        apiService.getTodaySleepLogs(dateQuery).enqueue(new Callback<List<SleepLog>>() {
            @Override
            public void onResponse(Call<List<SleepLog>> call, Response<List<SleepLog>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int totalMinutes = 0;
                    for (SleepLog log : response.body()) {
                        totalMinutes += log.getMinutes();
                    }
                    // 목표 시간 '이상'이면 성공
                    boolean isSuccess = totalMinutes >= sleepGoal;
                    tvSleepValue.setText(isSuccess ? "O" : "X");
                }
            }
            @Override
            public void onFailure(Call<List<SleepLog>> call, Throwable t) {}
        });
    }
}
