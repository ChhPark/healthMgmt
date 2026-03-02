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
        Calendar today = Calendar.getInstance(); 
        
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        while (!dayCal.after(endCal)) {
            if (dayCal.after(today)) {
                break;
            }
            String date = DATE_FORMAT.format(dayCal.getTime());
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
                                    if (total != null) report.setProteinSuccess(total <= goalLimit);
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
                        int amount = dailyTotals.getOrDefault(date, 0) + log.getSodiumAmount();
                        dailyTotals.put(date, amount);
                    }
                    for (DailyReport report : dailyReports) {
                        Integer total = dailyTotals.get(report.getDate());
                        if (total != null) report.setSodiumSuccess(total <= sodiumGoal);
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
                        int amount = dailyTotals.getOrDefault(date, 0) + log.getWaterAmount();
                        dailyTotals.put(date, amount);
                    }
                    for (DailyReport report : dailyReports) {
                        Integer total = dailyTotals.get(report.getDate());
                        if (total != null) report.setWaterSuccess(total >= waterGoal);
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
                    for (BeverageLog log : response.body()) {
                        if (!"디카페인 커피".equals(log.getBeverageType())) {
                            String date = log.getRecordDate();
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
                        int minutes = dailyTotals.getOrDefault(date, 0) + log.getMinutes();
                        dailyTotals.put(date, minutes);
                    }
                    for (DailyReport report : dailyReports) {
                        Integer total = dailyTotals.get(report.getDate());
                        if (total != null) report.setSleepSuccess(total >= sleepGoal);
                    }
                }
                onComplete.run();
            }
            @Override
            public void onFailure(Call<List<SleepLog>> call, Throwable t) { onComplete.run(); }
        });
    }

    // [수정] 걸음수 조건 포함 (10,000보 이상 또는 목표 시간 이상)
    private void loadExerciseData(String startDate, String endDate, Runnable onComplete) {
        SharedPreferences prefs = getSharedPreferences("HealthPrefs", Context.MODE_PRIVATE);
        int exerciseGoal = prefs.getInt("exercise_target", 30);
        String url = "/rest/v1/health_exercise?select=*&record_date=gte." + startDate + "&record_date=lte." + endDate;
        apiService.getExerciseLogsInRange(url).enqueue(new Callback<List<ExerciseLog>>() {
            @Override
            public void onResponse(Call<List<ExerciseLog>> call, Response<List<ExerciseLog>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // int[] -> index 0: 일반 운동 시간(분), index 1: 걸음수(보)
                    Map<String, int[]> dailyTotals = new HashMap<>();
                    
                    for (ExerciseLog log : response.body()) {
                        String date = log.getRecordDate();
                        dailyTotals.putIfAbsent(date, new int[]{0, 0});
                        
                        if ("걸음수".equals(log.getExerciseType())) {
                            dailyTotals.get(date)[1] += log.getMinutes();
                        } else {
                            dailyTotals.get(date)[0] += log.getMinutes();
                        }
                    }
                    
                    for (DailyReport report : dailyReports) {
                        int[] totals = dailyTotals.get(report.getDate());
                        if (totals != null) {
                            // 일반 운동 30분 이상 이거나, 걸음수 10000보 이상이면 성공
                            boolean isSuccess = (totals[0] >= exerciseGoal) || (totals[1] >= 10000);
                            report.setExerciseSuccess(isSuccess);
                        } else {
                            report.setExerciseSuccess(false);
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

            setItemText(row.getChildAt(2), report.isProteinSuccess());
            setItemText(row.getChildAt(3), report.isSodiumSuccess());
            setItemText(row.getChildAt(4), report.isWaterSuccess());
            setItemText(row.getChildAt(5), report.isBeverageSuccess());
            setItemText(row.getChildAt(6), report.isAlcoholSuccess());
            setItemText(row.getChildAt(7), report.isSleepSuccess());
            setItemText(row.getChildAt(8), report.isExerciseSuccess());

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

        private void setItemText(View view, boolean success) {
            TextView tv = (TextView) view;
            String icon = success ? "O" : "X";
            tv.setText(icon);
            int color = success ? Color.parseColor("#4CAF50") : Color.parseColor("#F44336"); 
            if(!success) color = Color.parseColor("#E0E0E0"); 
            tv.setTextColor(color);
        }
    }
}
