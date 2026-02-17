package com.house.healthMgmt;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    
    private TextView tvDateTitle;
    private TextView tvProteinValue;
    private TextView tvSodiumValue;
    private TextView tvWaterValue;
    private TextView tvNoBeverageValue;
    private TextView tvNoAlcoholValue;
    private TextView tvSleepValue;
    private TextView tvExerciseValue;
    
    private SupabaseApi apiService;
    
    private String todayDate;
    private Calendar currentCalendar = Calendar.getInstance();
    
    // ✅ [추가] SimpleDateFormat 재사용
    private static final SimpleDateFormat DATE_FORMAT_QUERY = 
        new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
    private static final SimpleDateFormat DATE_FORMAT_DISPLAY = 
        new SimpleDateFormat("yyyy년 MM월 dd일 EEEE", Locale.KOREA);

    @Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    
    // ✅ 월별 리포트에서 전달받은 날짜 확인
    Intent intent = getIntent();
    if (intent != null && intent.hasExtra("target_date")) {
        String targetDate = intent.getStringExtra("target_date");
        // 해당 날짜로 캘린더 설정
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
            Date date = sdf.parse(targetDate);
            currentCalendar.setTime(date);
            updateDateDisplay();
        } catch (Exception e) {
            // 파싱 실패 시 오늘 날짜 사용
        }
    }

    // UI 연결
    tvDateTitle = findViewById(R.id.tv_date_title);
    tvProteinValue = findViewById(R.id.tv_protein_value);
    tvSodiumValue = findViewById(R.id.tv_sodium_value);
    tvWaterValue = findViewById(R.id.tv_water_value);
    tvNoBeverageValue = findViewById(R.id.tv_no_beverage_value);
    tvNoAlcoholValue = findViewById(R.id.tv_no_alcohol_value);
    tvSleepValue = findViewById(R.id.tv_sleep_value);
    tvExerciseValue = findViewById(R.id.tv_exercise_value);

    apiService = SupabaseClient.getApi(this);

    updateDateDisplay(); 

    tvDateTitle.setOnLongClickListener(v -> {
        showDatePicker();
        return true;
    });

    setupCardListeners();
    
    // ✅ 월별 리포트 버튼 (수정됨)
    findViewById(R.id.btn_monthly_report).setOnClickListener(v -> 
        startActivity(new Intent(MainActivity.this, MonthlyReportActivity.class))
    );
}

    @Override
    protected void onResume() {
        super.onResume();
        refreshAllGoals();
    }

    private void setupCardListeners() {
        findViewById(R.id.card_protein).setOnClickListener(v -> navigateTo(ProteinActivity.class));
        findViewById(R.id.card_sodium).setOnClickListener(v -> navigateTo(SodiumActivity.class));
        findViewById(R.id.card_water).setOnClickListener(v -> navigateTo(WaterActivity.class));
        findViewById(R.id.card_no_beverage).setOnClickListener(v -> navigateTo(NoBeverageActivity.class));
        findViewById(R.id.card_no_alcohol).setOnClickListener(v -> navigateTo(NoAlcoholActivity.class));
        findViewById(R.id.card_sleep).setOnClickListener(v -> navigateTo(SleepActivity.class));
        findViewById(R.id.card_exercise).setOnClickListener(v -> navigateTo(ExerciseActivity.class));
    }

    private void navigateTo(Class<?> targetActivity) {
        Intent intent = new Intent(MainActivity.this, targetActivity);
        intent.putExtra("target_date", todayDate); 
        startActivity(intent);
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, year, month, dayOfMonth) -> {
                currentCalendar.set(year, month, dayOfMonth);
                updateDateDisplay();
                refreshAllGoals();
            },
            currentCalendar.get(Calendar.YEAR),
            currentCalendar.get(Calendar.MONTH),
            currentCalendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void updateDateDisplay() {
    String dateText = DATE_FORMAT_DISPLAY.format(currentCalendar.getTime());
    tvDateTitle.setText(dateText + " 📅"); // ✅ 달력 이모지 추가
    
    // ✅ 짧게 클릭하면 힌트 표시
    tvDateTitle.setOnClickListener(v -> {
        Toast.makeText(this, 
            "💡 팁: 날짜를 길게 누르면 달력이 나타납니다!", 
            Toast.LENGTH_LONG).show();
    });
        
        String selectedDateStr = DATE_FORMAT_QUERY.format(currentCalendar.getTime());
        this.todayDate = selectedDateStr;

        String realTodayStr = DATE_FORMAT_QUERY.format(new Date());
        if (selectedDateStr.equals(realTodayStr)) {
            tvDateTitle.setTextColor(Color.parseColor("#333333"));
        } else {
            tvDateTitle.setTextColor(Color.RED);
        }
    }

    private void refreshAllGoals() {
        checkProteinGoal();
        checkSodiumGoal();
        checkWaterGoal();
        checkBeverageGoal();
        checkAlcoholGoal();
        checkSleepGoal(); 
        checkExerciseGoal();
    }

    // ✅ [추가] 공통 에러 처리 메서드
    private void showError(String message) {
        runOnUiThread(() -> 
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        );
    }

    // 1. 단백질
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
                // ✅ [개선] 에러 처리
                Log.e(TAG, "Weight Error", t);
                tvProteinValue.setText("-");
                showError("체중 데이터를 불러올 수 없습니다.");
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
                    boolean isSuccess = totalProtein <= goalLimit; 
                    tvProteinValue.setText(isSuccess ? "O" : "X");
                } else {
                    tvProteinValue.setText("-");
                }
            }
            @Override
            public void onFailure(Call<List<ProteinLog>> call, Throwable t) {
                // ✅ [개선] 에러 처리
                Log.e(TAG, "Protein Error", t);
                tvProteinValue.setText("-");
            }
        });
    }

    // 2. 나트륨
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
                    boolean isSuccess = totalSodium <= sodiumGoal;
                    tvSodiumValue.setText(isSuccess ? "O" : "X");
                } else {
                    tvSodiumValue.setText("-");
                }
            }
            @Override
            public void onFailure(Call<List<SodiumLog>> call, Throwable t) {
                // ✅ [개선] 에러 처리
                Log.e(TAG, "Sodium Error", t);
                tvSodiumValue.setText("-");
            }
        });
    }

    // 3. 물
    private void checkWaterGoal() {
        SharedPreferences prefs = getSharedPreferences("HealthPrefs", Context.MODE_PRIVATE);
        int waterGoal = prefs.getInt("water_target", 2000);
        String dateQuery = "eq." + todayDate;

        apiService.getTodayWaterLogs(dateQuery).enqueue(new Callback<List<WaterLog>>() {
            @Override
            public void onResponse(Call<List<WaterLog>> call, Response<List<WaterLog>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int totalWater = 0;
                    for (WaterLog log : response.body()) {
                        totalWater += log.getWaterAmount();
                    }
                    boolean isSuccess = totalWater >= waterGoal;
                    tvWaterValue.setText(isSuccess ? "O" : "X");
                } else {
                    tvWaterValue.setText("-");
                }
            }
            @Override
            public void onFailure(Call<List<WaterLog>> call, Throwable t) {
                // ✅ [개선] 에러 처리
                Log.e(TAG, "Water Error", t);
                tvWaterValue.setText("-");
            }
        });
    }

    // 4. No음료수
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
                } else {
                    tvNoBeverageValue.setText("-");
                }
            }
            @Override
            public void onFailure(Call<List<BeverageLog>> call, Throwable t) {
                // ✅ [개선] 에러 처리
                Log.e(TAG, "Beverage Error", t);
                tvNoBeverageValue.setText("-");
            }
        });
    }

    // 5. No술
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
                } else {
                    tvNoAlcoholValue.setText("-");
                }
            }
            @Override
            public void onFailure(Call<List<AlcoholLog>> call, Throwable t) {
                // ✅ [개선] 에러 처리
                Log.e(TAG, "Alcohol Error", t);
                tvNoAlcoholValue.setText("-");
            }
        });
    }

    // 6. 수면
    private void checkSleepGoal() {
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
                    boolean isSuccess = totalMinutes >= sleepGoal;
                    tvSleepValue.setText(isSuccess ? "O" : "X");
                } else {
                    tvSleepValue.setText("-");
                }
            }
            @Override
            public void onFailure(Call<List<SleepLog>> call, Throwable t) {
                // ✅ [개선] 에러 처리
                Log.e(TAG, "Sleep Error", t);
                tvSleepValue.setText("-");
            }
        });
    }

    // 7. 운동
    private void checkExerciseGoal() {
        SharedPreferences prefs = getSharedPreferences("HealthPrefs", Context.MODE_PRIVATE);
        int exerciseGoal = prefs.getInt("exercise_target", 30);
        String dateQuery = "eq." + todayDate;

        apiService.getTodayExerciseLogs(dateQuery).enqueue(new Callback<List<ExerciseLog>>() {
            @Override
            public void onResponse(Call<List<ExerciseLog>> call, Response<List<ExerciseLog>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int totalMinutes = 0;
                    for (ExerciseLog log : response.body()) {
                        totalMinutes += log.getMinutes();
                    }
                    boolean isSuccess = totalMinutes >= exerciseGoal;
                    tvExerciseValue.setText(isSuccess ? "O" : "X");
                } else {
                    tvExerciseValue.setText("-");
                }
            }
            @Override
            public void onFailure(Call<List<ExerciseLog>> call, Throwable t) {
                // ✅ [개선] 에러 처리
                Log.e(TAG, "Exercise Error", t);
                tvExerciseValue.setText("-");
            }
        });
    }
}
