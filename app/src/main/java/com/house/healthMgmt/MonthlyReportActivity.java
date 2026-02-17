package com.house.healthMgmt;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MonthlyReportActivity extends AppCompatActivity {

    private TextView tvCurrentMonth;
    private TextView tvSuccessDays;
    private TextView tvTotalRate;
    private TextView tvPerfectDays;
    private ListView lvDailyReports;
    private ProgressBar progressBar;
    private TextView tvNoData;
    
    private SupabaseApi apiService;
    private Calendar currentCalendar = Calendar.getInstance();
    
    private List<DailyReport> dailyReports = new ArrayList<>();
    private DailyReportAdapter adapter;
    
    private static final SimpleDateFormat DATE_FORMAT = 
        new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
    private static final SimpleDateFormat MONTH_FORMAT = 
        new SimpleDateFormat("yyyy년 M월", Locale.KOREA);
    private static final SimpleDateFormat DAY_FORMAT = 
        new SimpleDateFormat("M월 d일 (E)", Locale.KOREA);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monthly_report);

        // UI 연결
        tvCurrentMonth = findViewById(R.id.tv_current_month);
        tvSuccessDays = findViewById(R.id.tv_success_days);
        tvTotalRate = findViewById(R.id.tv_total_rate);
        tvPerfectDays = findViewById(R.id.tv_perfect_days);
        lvDailyReports = findViewById(R.id.lv_daily_reports);
        progressBar = findViewById(R.id.progress_bar);
        tvNoData = findViewById(R.id.tv_no_data);

        apiService = SupabaseClient.getApi(this);

        // 어댑터 설정
        adapter = new DailyReportAdapter(this, dailyReports);
        lvDailyReports.setAdapter(adapter);

        // 리스트 아이템 클릭 시 해당 날짜의 MainActivity로 이동
        lvDailyReports.setOnItemClickListener((parent, view, position, id) -> {
            DailyReport report = dailyReports.get(position);
            Intent intent = new Intent(MonthlyReportActivity.this, MainActivity.class);
            intent.putExtra("target_date", report.getDate());
            startActivity(intent);
        });

        // 버튼 이벤트
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_prev_month).setOnClickListener(v -> changeMonth(-1));
        findViewById(R.id.btn_next_month).setOnClickListener(v -> changeMonth(1));

        // 현재 월 데이터 로드
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
        while (!dayCal.after(endCal)) {
            String date = DATE_FORMAT.format(dayCal.getTime());
            dailyReports.add(new DailyReport(date));
            dayCal.add(Calendar.DAY_OF_MONTH, 1);
        }

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

        int totalRate = dailyReports.isEmpty() ? 0 : 
            (int) ((totalSuccessCount * 100.0) / totalItemCount);

        tvSuccessDays.setText(String.valueOf(successDays));
        tvTotalRate.setText(totalRate + "%");
        tvPerfectDays.setText(String.valueOf(perfectDays));
    }

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
                                    int amount = dailyTotals.getOrDefault(date, 0) + log.getProteinAmount();
                                    dailyTotals.put(date, amount);
                                }
                                
                                for (DailyReport report : dailyReports) {
                                    Integer total = dailyTotals.get(report.getDate());
                                    if (total != null) {
                                        report.setProteinSuccess(total <= goalLimit);
                                    }
                                }
                            }
                            onComplete.run();
                        }
                        @Override
                        public void onFailure(Call<List<ProteinLog>> call, Throwable t) {
                            onComplete.run();
                        }
                    });
                } else {
                    onComplete.run();
                }
            }
            @Override
            public void onFailure(Call<List<WeightLog>> call, Throwable t) {
                onComplete.run();
            }
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
                        int amount = dailyTotals.getOrDefault(date, 0) + log.getSodiumAmount();
                        dailyTotals.put(date, amount);
                    }
                    
                    for (DailyReport report : dailyReports) {
                        Integer total = dailyTotals.get(report.getDate());
                        if (total != null) {
                            report.setSodiumSuccess(total <= sodiumGoal);
                        }
                    }
                }
                onComplete.run();
            }
            @Override
            public void onFailure(Call<List<SodiumLog>> call, Throwable t) {
                onComplete.run();
            }
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
                        int amount = dailyTotals.getOrDefault(date, 0) + log.getWaterAmount();
                        dailyTotals.put(date, amount);
                    }
                    
                    for (DailyReport report : dailyReports) {
                        Integer total = dailyTotals.get(report.getDate());
                        if (total != null) {
                            report.setWaterSuccess(total >= waterGoal);
                        }
                    }
                }
                onComplete.run();
            }
            @Override
            public void onFailure(Call<List<WaterLog>> call, Throwable t) {
                onComplete.run();
            }
        });
    }

    private void loadBeverageData(String startDate, String endDate, Runnable onComplete) {
		
    String url = "/rest/v1/health_beverage?select=*&record_date=gte." + startDate + "&record_date=lte." + endDate;
    
    apiService.getBeverageLogsInRange(url).enqueue(new Callback<List<BeverageLog>>() {
		@Override
            public void onResponse(Call<List<BeverageLog>> call, Response<List<BeverageLog>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Integer> dailyTotals = new HashMap<>();
                    for (BeverageLog log : response.body()) {
                        String date = log.getRecordDate();
                        if (!"디카페인 커피".equals(log.getBeverageType())) {
                            int amount = dailyTotals.getOrDefault(date, 0) + log.getAmount();
                            dailyTotals.put(date, amount);
                        }
                    }
                    
                    for (DailyReport report : dailyReports) {
                        Integer total = dailyTotals.get(report.getDate());
                        report.setBeverageSuccess(total == null || total == 0);
                    }
                }
                onComplete.run();
            }
            @Override
            public void onFailure(Call<List<BeverageLog>> call, Throwable t) {
                onComplete.run();
            }
        });
    }

    private void loadAlcoholData(String startDate, String endDate, Runnable onComplete) {
    String url = "/rest/v1/health_alcohol?select=*&record_date=gte." + startDate + "&record_date=lte." + endDate;  // ✅
    
    apiService.getAlcoholLogsInRange(url).enqueue(new Callback<List<AlcoholLog>>() {  // ✅
		@Override
            public void onResponse(Call<List<AlcoholLog>> call, Response<List<AlcoholLog>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Integer> dailyTotals = new HashMap<>();
                    for (AlcoholLog log : response.body()) {
                        String date = log.getRecordDate();
                        int amount = dailyTotals.getOrDefault(date, 0) + log.getAmount();
                        dailyTotals.put(date, amount);
                    }
                    
                    for (DailyReport report : dailyReports) {
                        Integer total = dailyTotals.get(report.getDate());
                        report.setAlcoholSuccess(total == null || total == 0);
                    }
                }
                onComplete.run();
            }
            @Override
            public void onFailure(Call<List<AlcoholLog>> call, Throwable t) {
                onComplete.run();
            }
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
                        int minutes = dailyTotals.getOrDefault(date, 0) + log.getMinutes();
                        dailyTotals.put(date, minutes);
                    }
                    
                    for (DailyReport report : dailyReports) {
                        Integer total = dailyTotals.get(report.getDate());
                        if (total != null) {
                            report.setSleepSuccess(total >= sleepGoal);
                        }
                    }
                }
                onComplete.run();
            }
            @Override
            public void onFailure(Call<List<SleepLog>> call, Throwable t) {
                onComplete.run();
            }
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
                    Map<String, Integer> dailyTotals = new HashMap<>();
                    for (ExerciseLog log : response.body()) {
                        String date = log.getRecordDate();
                        int minutes = dailyTotals.getOrDefault(date, 0) + log.getMinutes();
                        dailyTotals.put(date, minutes);
                    }
                    
                    for (DailyReport report : dailyReports) {
                        Integer total = dailyTotals.get(report.getDate());
                        if (total != null) {
                            report.setExerciseSuccess(total >= exerciseGoal);
                        }
                    }
                }
                onComplete.run();
            }
            @Override
            public void onFailure(Call<List<ExerciseLog>> call, Throwable t) {
                onComplete.run();
            }
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
                convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_daily_report, parent, false);
            }

            DailyReport report = getItem(position);
            if (report == null) return convertView;

            TextView tvDate = convertView.findViewById(R.id.tv_date);
            try {
                Calendar cal = Calendar.getInstance();
                cal.setTime(DATE_FORMAT.parse(report.getDate()));
                tvDate.setText(DAY_FORMAT.format(cal.getTime()));
            } catch (Exception e) {
                tvDate.setText(report.getDate());
            }

            TextView tvSuccessCount = convertView.findViewById(R.id.tv_success_count);
            tvSuccessCount.setText(report.getSuccessCount() + "/" + report.getTotalCount());

            TextView tvGrade = convertView.findViewById(R.id.tv_grade);
            tvGrade.setText(report.getGrade());
            
            int rate = report.getSuccessRate();
            int gradeColor;
            if (rate == 100) gradeColor = Color.parseColor("#4CAF50");
            else if (rate >= 50) gradeColor = Color.parseColor("#FF9800");
            else gradeColor = Color.parseColor("#F44336");
            tvGrade.setTextColor(gradeColor);

   setItemText(convertView, R.id.tv_protein, report.isProteinSuccess());
setItemText(convertView, R.id.tv_sodium, report.isSodiumSuccess());
setItemText(convertView, R.id.tv_water, report.isWaterSuccess());
setItemText(convertView, R.id.tv_beverage, report.isBeverageSuccess());
setItemText(convertView, R.id.tv_alcohol, report.isAlcoholSuccess());
setItemText(convertView, R.id.tv_sleep, report.isSleepSuccess());
setItemText(convertView, R.id.tv_exercise, report.isExerciseSuccess());

            return convertView;
        }

private void setItemText(View view, int viewId, boolean success) {
    TextView tv = view.findViewById(viewId);
    String icon = success ? "O" : "X";  // ✅ O/X로 변경 (MainActivity와 동일)
    tv.setText(icon);
    
    int color = success ? Color.parseColor("#4CAF50") : Color.parseColor("#F44336");
    tv.setTextColor(color);
}
    }
}
