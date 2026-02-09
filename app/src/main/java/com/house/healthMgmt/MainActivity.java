package com.house.healthMgmt;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
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

    // ... (기존 TextView 변수들 동일) ...
    private TextView tvDateTitle;
    private TextView tvProteinValue;
    private TextView tvSodiumValue;
    private TextView tvWaterValue;
    private TextView tvNoBeverageValue;
    private TextView tvNoAlcoholValue;
    private TextView tvSleepValue;
    private TextView tvExerciseValue;
    
    private SupabaseApi apiService;
    
    // [수정] 날짜 관련 변수 통합 관리
    private String todayDate; // API 쿼리에 사용되는 날짜 문자열 (yyyy-MM-dd)
    private Calendar currentCalendar = Calendar.getInstance();

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
        tvExerciseValue = findViewById(R.id.tv_exercise_value);

        apiService = SupabaseClient.getApi(this);

        // 2. 초기 날짜 설정 (오늘 날짜로 초기화 및 UI 표시)
        updateDateDisplay(); 

        // 3. 날짜 롱클릭 리스너 (달력 띄우기)
        tvDateTitle.setOnLongClickListener(v -> {
            showDatePicker();
            return true;
        });

        // 4. 카드 클릭 리스너 설정 (화면 이동 + 날짜 전달)
        // [중요] 중복 정의를 제거하고 아래와 같이 통합했습니다.
        setupCardListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 화면이 다시 보일 때마다 현재 설정된 날짜(todayDate) 기준으로 데이터 갱신
        refreshAllGoals();
    }

    // [수정] 카드 클릭 리스너 통합 메서드
    private void setupCardListeners() {
        // 단백질
        findViewById(R.id.card_protein).setOnClickListener(v -> navigateTo(ProteinActivity.class));
        
        // 나트륨
        findViewById(R.id.card_sodium).setOnClickListener(v -> navigateTo(SodiumActivity.class));
        
        // 물
        findViewById(R.id.card_water).setOnClickListener(v -> navigateTo(WaterActivity.class));
        
        // No음료수
        findViewById(R.id.card_no_beverage).setOnClickListener(v -> navigateTo(NoBeverageActivity.class));
        
        // No술
        findViewById(R.id.card_no_alcohol).setOnClickListener(v -> navigateTo(NoAlcoholActivity.class));
        
        // 수면
        findViewById(R.id.card_sleep).setOnClickListener(v -> navigateTo(SleepActivity.class));
        
        // 운동
        findViewById(R.id.card_exercise).setOnClickListener(v -> navigateTo(ExerciseActivity.class));
    }

    // [추가] 화면 이동 공통 메서드 (날짜 전달 포함)
    private void navigateTo(Class<?> targetActivity) {
        Intent intent = new Intent(MainActivity.this, targetActivity);
        // ★ 핵심: 현재 선택된 날짜(todayDate)를 넘겨줌
        intent.putExtra("target_date", todayDate); 
        startActivity(intent);
    }

    // [수정] 달력 다이얼로그 표시
    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, year, month, dayOfMonth) -> {
                // 선택한 날짜로 Calendar 객체 갱신
                currentCalendar.set(year, month, dayOfMonth);
                
                // UI 업데이트 및 날짜 변수 갱신
                updateDateDisplay();
                
                // 변경된 날짜로 대시보드 O/X 데이터 다시 조회
                refreshAllGoals();
            },
            currentCalendar.get(Calendar.YEAR),
            currentCalendar.get(Calendar.MONTH),
            currentCalendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    // [수정] 날짜 UI 표시 및 변수 동기화 로직
    private void updateDateDisplay() {
        // 1. 보여줄 형식 (예: 2026년 02월 09일)
        SimpleDateFormat sdfDisplay = new SimpleDateFormat("yyyy년 MM월 dd일 EEEE", Locale.KOREA);
        tvDateTitle.setText(sdfDisplay.format(currentCalendar.getTime()));

        // 2. 서버 전송용 형식 (yyyy-MM-dd) 업데이트
        SimpleDateFormat sdfQuery = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
        String selectedDateStr = sdfQuery.format(currentCalendar.getTime());
        
        // [중요] API 쿼리에 쓰이는 변수(todayDate)를 선택된 날짜로 업데이트
        this.todayDate = selectedDateStr;

        // 3. 오늘 날짜와 비교하여 색상 변경
        String realTodayStr = sdfQuery.format(new Date());
        if (selectedDateStr.equals(realTodayStr)) {
            tvDateTitle.setTextColor(Color.parseColor("#333333")); // 오늘이면 검정
        } else {
            tvDateTitle.setTextColor(Color.RED); // 오늘이 아니면 빨강
        }
    }

    // --- [데이터 갱신 메서드 모음] ---
    private void refreshAllGoals() {
        checkProteinGoal();
        checkSodiumGoal();
        checkWaterGoal();
        checkBeverageGoal();
        checkAlcoholGoal();
        checkSleepGoal(); 
        checkExerciseGoal();
    }

    // --- [아래 check...Goal 메서드들은 기존 소스 그대로 유지] ---
    // todayDate 변수가 updateDateDisplay()에 의해 변경되었으므로, 
    // 아래 메서드들은 자동으로 선택된 날짜의 데이터를 조회하게 됩니다.

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
                Log.e("MainActivity", "Weight Error", t);
            }
        });
    }

    private void calculateProteinStatus(double weight) {
        int goalLimit = (int) Math.round(weight * 0.7);
        // todayDate는 이제 선택된 날짜입니다.
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
                }
            }
            @Override
            public void onFailure(Call<List<ProteinLog>> call, Throwable t) {}
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
                }
            }
            @Override
            public void onFailure(Call<List<SodiumLog>> call, Throwable t) {}
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
                }
            }
            @Override
            public void onFailure(Call<List<WaterLog>> call, Throwable t) {}
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
                }
            }
            @Override
            public void onFailure(Call<List<BeverageLog>> call, Throwable t) {}
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
                }
            }
            @Override
            public void onFailure(Call<List<AlcoholLog>> call, Throwable t) {}
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
                }
            }
            @Override
            public void onFailure(Call<List<SleepLog>> call, Throwable t) {}
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
                }
            }
            @Override
            public void onFailure(Call<List<ExerciseLog>> call, Throwable t) {}
        });
    }
}
