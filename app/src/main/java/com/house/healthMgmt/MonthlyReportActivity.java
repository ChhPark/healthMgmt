package com.house.healthMgmt;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.widget.Toast;


public class MonthlyReportActivity extends AppCompatActivity {

    private TextView tvCurrentMonth;
    private TextView tvSuccessDays;
    private TextView tvTotalRate;
    private TextView tvPerfectDays;
    private ListView lvDailyReports;
    private ProgressBar progressBar;
    private TextView tvNoData;
    private TextView btnToggleMode; // [추가] 모드 전환 버튼
    
    private SupabaseApi apiService;
    private Calendar currentCalendar = Calendar.getInstance();
    
    private List<DailyReport> dailyReports = new ArrayList<>();
    private DailyReportAdapter adapter;
    
    // [추가] 상태 모드 플래그 (false = O/X 모드, true = 수치 모드)
    private boolean isValueMode = false;
    
    private static final SimpleDateFormat DATE_FORMAT = 
        new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
    private static final SimpleDateFormat MONTH_FORMAT = 
        new SimpleDateFormat("yyyy년 M월", Locale.KOREA);
    private static final SimpleDateFormat DAY_FORMAT_SHORT = 
        new SimpleDateFormat("d일 (E)", Locale.KOREA);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monthly_report);

        tvCurrentMonth = findViewById(R.id.tv_current_month);
        tvSuccessDays = findViewById(R.id.tv_success_days);
        tvTotalRate = findViewById(R.id.tv_total_rate);
        tvPerfectDays = findViewById(R.id.tv_perfect_days);
        lvDailyReports = findViewById(R.id.lv_daily_reports);
        progressBar = findViewById(R.id.progress_bar);
        tvNoData = findViewById(R.id.tv_no_data);
        btnToggleMode = findViewById(R.id.btn_toggle_mode); // [추가]

        apiService = SupabaseClient.getApi(this);

        adapter = new DailyReportAdapter(this, dailyReports);
        lvDailyReports.setAdapter(adapter);

        lvDailyReports.setOnItemClickListener((parent, view, position, id) -> {
            DailyReport report = dailyReports.get(position);
            Intent intent = new Intent(MonthlyReportActivity.this, MainActivity.class);
            intent.putExtra("target_date", report.getDate());
            startActivity(intent);
        });

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_prev_month).setOnClickListener(v -> changeMonth(-1));
        findViewById(R.id.btn_next_month).setOnClickListener(v -> changeMonth(1));

        
                // [수정] 텍스트 변경 없이 상태만 전환하고 사용자에게 알림 제공
        btnToggleMode.setOnClickListener(v -> {
            isValueMode = !isValueMode;
            adapter.notifyDataSetChanged();
            
            // 토스트 메시지로 현재 모드 안내
            String modeName = isValueMode ? "수치 모드" : "O/X 모드";
            Toast.makeText(MonthlyReportActivity.this, modeName + "로 전환되었습니다.", Toast.LENGTH_SHORT).show();
        });


        updateMonthDisplay();
        loadMonthlyData();
    }

    private void updateMonthDisplay() {
        tvCurrentMonth.setText(MONTH_FORMAT.format(currentCalendar.getTime()));
    }

    private void changeMonth(int delta) {
        currentCalendar.add(Calendar.MONTH, delta);
        updateMonthDisplay();
        loadMonthlyData();
    }
    private void loadMonthlyData() {
        progressBar.setVisibility(View.VISIBLE);
        lvDailyReports.setVisibility(View.GONE);
        tvNoData.setVisibility(View.GONE);

        Calendar startCal = (Calendar) currentCalendar.clone();
        startCal.set(Calendar.DAY_OF_MONTH, 1);
        String startDate = DATE_FORMAT.format(startCal.getTime());

        Calendar endCal = (Calendar) currentCalendar.clone();
        endCal.set(Calendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH));
        String endDate = DATE_FORMAT.format(endCal.getTime());

        dailyReports.clear();
        Calendar dayCal = (Calendar) startCal.clone();
        
        // [수정] 복잡한 시간(Time) 비교 대신, 오늘 날짜를 문자열로 가져와 확실하게 비교합니다.
        String todayStr = DATE_FORMAT.format(new Date()); 

        while (!dayCal.after(endCal)) {
            String date = DATE_FORMAT.format(dayCal.getTime());
            
            // 날짜 문자열 비교: 루프의 날짜(date)가 오늘(todayStr)보다 미래 날짜라면 생성 중단
            if (date.compareTo(todayStr) > 0) {
                break;
            }
            
            dailyReports.add(new DailyReport(date));
            dayCal.add(Calendar.DAY_OF_MONTH, 1);
        }

        Collections.reverse(dailyReports);

        final int[] completedCalls = {0};
        final int totalCalls = 7;

        loadProteinData(startDate, endDate, () -> checkLoadComplete(++completedCalls[0], totalCalls));
        loadSodiumData(startDate, endDate, () -> checkLoadComplete(++completedCalls[0], totalCalls));
        loadWaterData(startDate, endDate, () -> checkLoadComplete(++completedCalls[0], totalCalls));
        loadBeverageData(startDate, endDate, () -> checkLoadComplete(++completedCalls[0], totalCalls));
        loadAlcoholData(startDate, endDate, () -> checkLoadComplete(++completedCalls[0], totalCalls));
        loadSleepData(startDate, endDate, () -> checkLoadComplete(++completedCalls[0], totalCalls));
        loadExerciseData(startDate, endDate, () -> checkLoadComplete(++completedCalls[0], totalCalls));
    }


    private void checkLoadComplete(int completed, int total) {
        if (completed >= total) {
            progressBar.setVisibility(View.GONE);
            
            if (dailyReports.isEmpty()) {
                tvNoData.setVisibility(View.VISIBLE);
                tvSuccessDays.setText("0");
                tvTotalRate.setText("0%");
                tvPerfectDays.setText("0");
            } else {
                lvDailyReports.setVisibility(View.VISIBLE);
                adapter.notifyDataSetChanged();
                calculateStatistics();
            }
        }
    }

    private void calculateStatistics() {
        int successDays = 0; 
        int perfectDays = 0;
        int totalSuccessCount = 0;
        int totalItemCount = dailyReports.size() * 7; 

        for (DailyReport report : dailyReports) {
            if (report.getSuccessCount() > 0) {
                successDays++;
            }
            if (report.getSuccessCount() == 7) {
                perfectDays++;
            }
            totalSuccessCount += report.getSuccessCount();
        }

        int totalRate = (totalItemCount == 0) ? 0 : 
            (int) ((totalSuccessCount * 100.0) / totalItemCount);

        tvSuccessDays.setText(String.valueOf(successDays));
        tvTotalRate.setText(totalRate + "%");
        tvPerfectDays.setText(String.valueOf(perfectDays));
    }

    // 각 로드 함수에 setAmount 추가
    private void loadProteinData(String startDate, String endDate, Runnable onComplete) {
        apiService.getLatestWeight().enqueue(new Callback<List<WeightLog>>() {
            @Override
            public void onResponse(Call<List<WeightLog>> call, Response<List<WeightLog>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    double weight = response.body().get(0).getWeight();
                    int goalLimit = (int) Math.round(weight * 0.7);
                    
                    String url = "/rest/v1/health_protein?select=*&record_date=gte." + startDate + "&record_date=lte." + endDate;
                    apiService.getProteinLogsInRange(url).enqueue(new Callback<List<ProteinLog>>() {
                        @Override
                        public void onResponse(Call<List<ProteinLog>> call, Response<List<ProteinLog>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                Map<String, Integer> dailyTotals = new HashMap<>();
                                for (ProteinLog log : response.body()) {
                                    String date = log.getRecordDate();
                                    dailyTotals.put(date, dailyTotals.getOrDefault(date, 0) + log.getProteinAmount());
                                }
                                for (DailyReport report : dailyReports) {
                                    if (dailyTotals.containsKey(report.getDate())) {
                                        int total = dailyTotals.get(report.getDate());
                                        report.setProteinSuccess(total <= goalLimit);
                                        report.setProteinAmount(total); // 수치 저장
                                    } else {
                                        report.setProteinSuccess(null); 
                                    }
                                }
                            }
                            onComplete.run();
                        }
                        @Override
                        public void onFailure(Call<List<ProteinLog>> call, Throwable t) { onComplete.run(); }
                    });
                } else { onComplete.run(); }
            }
            @Override
            public void onFailure(Call<List<WeightLog>> call, Throwable t) { onComplete.run(); }
        });
    }

    private void loadSodiumData(String startDate, String endDate, Runnable onComplete) {
        int sodiumGoal = 2000;
        String url = "/rest/v1/health_sodium?select=*&record_date=gte." + startDate + "&record_date=lte." + endDate;
        apiService.getSodiumLogsInRange(url).enqueue(new Callback<List<SodiumLog>>() {
            @Override
            public void onResponse(Call<List<SodiumLog>> call, Response<List<SodiumLog>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Integer> dailyTotals = new HashMap<>();
                    for (SodiumLog log : response.body()) {
                        String date = log.getRecordDate();
                        dailyTotals.put(date, dailyTotals.getOrDefault(date, 0) + log.getSodiumAmount());
                    }
                    for (DailyReport report : dailyReports) {
                        if (dailyTotals.containsKey(report.getDate())) {
                            int total = dailyTotals.get(report.getDate());
                            report.setSodiumSuccess(total <= sodiumGoal);
                            report.setSodiumAmount(total);
                        } else {
                            report.setSodiumSuccess(null);
                        }
                    }
                }
                onComplete.run();
            }
            @Override
            public void onFailure(Call<List<SodiumLog>> call, Throwable t) { onComplete.run(); }
        });
    }

    private void loadWaterData(String startDate, String endDate, Runnable onComplete) {
        SharedPreferences prefs = getSharedPreferences("HealthPrefs", Context.MODE_PRIVATE);
        int waterGoal = prefs.getInt("water_target", 2000);
        String url = "/rest/v1/health_water?select=*&record_date=gte." + startDate + "&record_date=lte." + endDate;
        apiService.getWaterLogsInRange(url).enqueue(new Callback<List<WaterLog>>() {
            @Override
            public void onResponse(Call<List<WaterLog>> call, Response<List<WaterLog>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Integer> dailyTotals = new HashMap<>();
                    for (WaterLog log : response.body()) {
                        String date = log.getRecordDate();
                        dailyTotals.put(date, dailyTotals.getOrDefault(date, 0) + log.getWaterAmount());
                    }
                    for (DailyReport report : dailyReports) {
                        if (dailyTotals.containsKey(report.getDate())) {
                            int total = dailyTotals.get(report.getDate());
                            report.setWaterSuccess(total >= waterGoal);
                            report.setWaterAmount(total);
                        } else {
                            report.setWaterSuccess(null);
                        }
                    }
                }
                onComplete.run();
            }
            @Override
            public void onFailure(Call<List<WaterLog>> call, Throwable t) { onComplete.run(); }
        });
    }

    private void loadBeverageData(String startDate, String endDate, Runnable onComplete) {
        String url = "/rest/v1/health_beverage?select=*&record_date=gte." + startDate + "&record_date=lte." + endDate;
        apiService.getBeverageLogsInRange(url).enqueue(new Callback<List<BeverageLog>>() {
            @Override
            public void onResponse(Call<List<BeverageLog>> call, Response<List<BeverageLog>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Integer> dailyTotals = new HashMap<>();
                    Set<String> recordedDates = new HashSet<>(); 
                    
                    for (BeverageLog log : response.body()) {
                        String date = log.getRecordDate();
                        recordedDates.add(date);
                        if (!"디카페인 커피".equals(log.getBeverageType())) {
                            dailyTotals.put(date, dailyTotals.getOrDefault(date, 0) + log.getAmount());
                        }
                    }
                    for (DailyReport report : dailyReports) {
                        if (recordedDates.contains(report.getDate())) {
                            int total = dailyTotals.getOrDefault(report.getDate(), 0);
                            report.setBeverageSuccess(total == 0);
                            report.setBeverageAmount(total);
                        } else {
                            report.setBeverageSuccess(null); 
                        }
                    }
                }
                onComplete.run();
            }
            @Override
            public void onFailure(Call<List<BeverageLog>> call, Throwable t) { onComplete.run(); }
        });
    }

    private void loadAlcoholData(String startDate, String endDate, Runnable onComplete) {
        String url = "/rest/v1/health_alcohol?select=*&record_date=gte." + startDate + "&record_date=lte." + endDate;
        apiService.getAlcoholLogsInRange(url).enqueue(new Callback<List<AlcoholLog>>() {
            @Override
            public void onResponse(Call<List<AlcoholLog>> call, Response<List<AlcoholLog>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Integer> dailyTotals = new HashMap<>();
                    Set<String> recordedDates = new HashSet<>();
                    
                    for (AlcoholLog log : response.body()) {
                        String date = log.getRecordDate();
                        recordedDates.add(date);
                        dailyTotals.put(date, dailyTotals.getOrDefault(date, 0) + log.getAmount());
                    }
                    for (DailyReport report : dailyReports) {
                        if (recordedDates.contains(report.getDate())) {
                            int total = dailyTotals.getOrDefault(report.getDate(), 0);
                            report.setAlcoholSuccess(total == 0);
                            report.setAlcoholAmount(total);
                        } else {
                            report.setAlcoholSuccess(null);
                        }
                    }
                }
                onComplete.run();
            }
            @Override
            public void onFailure(Call<List<AlcoholLog>> call, Throwable t) { onComplete.run(); }
        });
    }

    private void loadSleepData(String startDate, String endDate, Runnable onComplete) {
        SharedPreferences prefs = getSharedPreferences("HealthPrefs", Context.MODE_PRIVATE);
        int sleepGoal = prefs.getInt("sleep_target", 420);
        String url = "/rest/v1/health_sleep?select=*&record_date=gte." + startDate + "&record_date=lte." + endDate;
        apiService.getSleepLogsInRange(url).enqueue(new Callback<List<SleepLog>>() {
            @Override
            public void onResponse(Call<List<SleepLog>> call, Response<List<SleepLog>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Integer> dailyTotals = new HashMap<>();
                    for (SleepLog log : response.body()) {
                        String date = log.getRecordDate();
                        dailyTotals.put(date, dailyTotals.getOrDefault(date, 0) + log.getMinutes());
                    }
                    for (DailyReport report : dailyReports) {
                        if (dailyTotals.containsKey(report.getDate())) {
                            int total = dailyTotals.get(report.getDate());
                            report.setSleepSuccess(total >= sleepGoal);
                            report.setSleepMinutes(total);
                        } else {
                            report.setSleepSuccess(null);
                        }
                    }
                }
                onComplete.run();
            }
            @Override
            public void onFailure(Call<List<SleepLog>> call, Throwable t) { onComplete.run(); }
        });
    }

    private void loadExerciseData(String startDate, String endDate, Runnable onComplete) {
        SharedPreferences prefs = getSharedPreferences("HealthPrefs", Context.MODE_PRIVATE);
        int exerciseGoal = prefs.getInt("exercise_target", 30);
        String url = "/rest/v1/health_exercise?select=*&record_date=gte." + startDate + "&record_date=lte." + endDate;
        apiService.getExerciseLogsInRange(url).enqueue(new Callback<List<ExerciseLog>>() {
            @Override
            public void onResponse(Call<List<ExerciseLog>> call, Response<List<ExerciseLog>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, int[]> dailyTotals = new HashMap<>();
                    Set<String> recordedDates = new HashSet<>();
                    
                    for (ExerciseLog log : response.body()) {
                        String date = log.getRecordDate();
                        recordedDates.add(date);
                        dailyTotals.putIfAbsent(date, new int[]{0, 0});
                        
                        if ("걸음수".equals(log.getExerciseType())) {
                            dailyTotals.get(date)[1] += log.getMinutes();
                        } else {
                            dailyTotals.get(date)[0] += log.getMinutes();
                        }
                    }
                    
                    for (DailyReport report : dailyReports) {
                        if (recordedDates.contains(report.getDate())) {
                            int[] totals = dailyTotals.get(report.getDate());
                            boolean isSuccess = (totals != null) && ((totals[0] >= exerciseGoal) || (totals[1] >= 10000));
                            report.setExerciseSuccess(isSuccess);
                            report.setExerciseMinutes(totals != null ? totals[0] : 0);
                            report.setExerciseSteps(totals != null ? totals[1] : 0);
                        } else {
                            report.setExerciseSuccess(null);
                        }
                    }
                }
                onComplete.run();
            }
            @Override
            public void onFailure(Call<List<ExerciseLog>> call, Throwable t) { onComplete.run(); }
        });
    }

    private class DailyReportAdapter extends ArrayAdapter<DailyReport> {
        
        public DailyReportAdapter(Context context, List<DailyReport> reports) {
            super(context, 0, reports);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = createTableRow(parent.getContext());
            }

            DailyReport report = getItem(position);
            if (report == null) return convertView;

            LinearLayout row = (LinearLayout) convertView;
            
            TextView tvDate = (TextView) row.getChildAt(0);
            try {
                Calendar cal = Calendar.getInstance();
                cal.setTime(DATE_FORMAT.parse(report.getDate()));
                tvDate.setText(DAY_FORMAT_SHORT.format(cal.getTime()));
            } catch (Exception e) {
                tvDate.setText(report.getDate().substring(5));
            }

            TextView tvCount = (TextView) row.getChildAt(1);
            tvCount.setText(report.getSuccessCount() + "/7");
            if(report.getSuccessCount() == 7) tvCount.setTextColor(Color.parseColor("#4CAF50"));
            else tvCount.setTextColor(Color.parseColor("#666666"));

            // [수정] 수치 모드 지원을 위해 updateCell 메서드 사용
            updateCell((TextView) row.getChildAt(2), report.isProteinSuccess(), report.getProteinAmount());
            updateCell((TextView) row.getChildAt(3), report.isSodiumSuccess(), report.getSodiumAmount());
            updateCell((TextView) row.getChildAt(4), report.isWaterSuccess(), report.getWaterAmount());
            updateCell((TextView) row.getChildAt(5), report.isBeverageSuccess(), report.getBeverageAmount());
            updateCell((TextView) row.getChildAt(6), report.isAlcoholSuccess(), report.getAlcoholAmount());
            updateCell((TextView) row.getChildAt(7), report.isSleepSuccess(), report.getSleepMinutes());
            
            // 운동은 분/보 두 가지 수치를 넘겨줌
            updateExerciseCell((TextView) row.getChildAt(8), report.isExerciseSuccess(), report.getExerciseMinutes(), report.getExerciseSteps());

            return convertView;
        }

        private View createTableRow(Context context) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 24, 0, 24); 
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackgroundColor(Color.WHITE);

            TextView tvDate = new TextView(context);
            LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2.5f);
            tvDate.setLayoutParams(p1);
            tvDate.setGravity(Gravity.CENTER);
            tvDate.setTextSize(13);
            tvDate.setTextColor(Color.BLACK);
            row.addView(tvDate);

            TextView tvCount = new TextView(context);
            LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.5f);
            tvCount.setLayoutParams(p2);
            tvCount.setGravity(Gravity.CENTER);
            tvCount.setTextSize(13);
            tvCount.setTypeface(null, android.graphics.Typeface.BOLD);
            row.addView(tvCount);

            for(int i=0; i<7; i++) {
                TextView tvItem = new TextView(context);
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                tvItem.setLayoutParams(p);
                tvItem.setGravity(Gravity.CENTER);
                tvItem.setTextSize(14);
                row.addView(tvItem);
            }

            return row;
        }

        // [수정] 일반 항목 셀 업데이트 메서드 (수치에 콤마 및 색상 적용)
        private void updateCell(TextView tv, Boolean success, Integer value) {
            if (success == null) {
                // 기록이 없는 경우
                tv.setText("-");
                tv.setTextColor(Color.parseColor("#B0B0B0")); 
                tv.setTypeface(null, android.graphics.Typeface.NORMAL);
                tv.setTextSize(14);
                return;
            }

            // 공통 색상 결정 (성공=초록, 실패=빨강)
            int textColor = success ? Color.parseColor("#4CAF50") : Color.parseColor("#F44336");

            if (isValueMode) {
                // 수치 보기 모드 (콤마 적용)
                tv.setText(value != null ? String.format(Locale.KOREA, "%,d", value) : "0");
                tv.setTextColor(textColor); 
                tv.setTypeface(null, android.graphics.Typeface.BOLD); // 색상이 들어가므로 BOLD 처리
                tv.setTextSize(11); 
            } else {
                // O/X 보기 모드
                tv.setText(success ? "O" : "X");
                tv.setTextColor(textColor); 
                tv.setTypeface(null, android.graphics.Typeface.BOLD);
                tv.setTextSize(14);
            }
        }

        // [수정] 운동 셀 전용 업데이트 메서드 (수치에 콤마 및 색상 적용)
        private void updateExerciseCell(TextView tv, Boolean success, Integer minutes, Integer steps) {
            if (success == null) {
                tv.setText("-");
                tv.setTextColor(Color.parseColor("#B0B0B0")); 
                tv.setTypeface(null, android.graphics.Typeface.NORMAL);
                tv.setTextSize(14);
                return;
            }

            // 공통 색상 결정
            int textColor = success ? Color.parseColor("#4CAF50") : Color.parseColor("#F44336");

            if (isValueMode) {
                String text = "";
                int m = (minutes != null) ? minutes : 0;
                int s = (steps != null) ? steps : 0;
                
                if (m > 0) text += String.format(Locale.KOREA, "%,d", m);
                
                if (s > 0) {
                    if (!text.isEmpty()) text += "\n"; // 일반 운동이 있으면 줄바꿈 후 걸음수
                    
                    if (s >= 10000) {
                        text += (s / 10000) + "만"; // 예: 10000 -> 1만
                    } else {
                        // 1만보 미만은 천 단위 콤마 찍기
                        text += String.format(Locale.KOREA, "%,d", s); 
                    }
                }
                
                if (text.isEmpty()) text = "0";

                tv.setText(text);
                tv.setTextColor(textColor); // 수치에도 O/X와 동일한 색상 적용
                tv.setTypeface(null, android.graphics.Typeface.BOLD);
                tv.setTextSize(10); 
            } else {
                tv.setText(success ? "O" : "X");
                tv.setTextColor(textColor); 
                tv.setTypeface(null, android.graphics.Typeface.BOLD);
                tv.setTextSize(14);
            }
        }

    }
}
