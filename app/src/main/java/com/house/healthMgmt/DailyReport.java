package com.house.healthMgmt;

/**
 * 일별 리포트 데이터 모델
 * 하루의 모든 건강 항목 성공/실패 정보 및 실제 수치를 담음
 */
public class DailyReport {
    
    private String date;              // 날짜 (yyyy-MM-dd)
    
    // 목표 달성 여부 (null 허용: 데이터 없음)
    private Boolean proteinSuccess = null;
    private Boolean sodiumSuccess = null;
    private Boolean waterSuccess = null;
    private Boolean beverageSuccess = null;
    private Boolean alcoholSuccess = null;
    private Boolean sleepSuccess = null;
    private Boolean exerciseSuccess = null;

    // [추가] 실제 데이터 수치 (null 허용)
    private Integer proteinAmount = null;
    private Integer sodiumAmount = null;
    private Integer waterAmount = null;
    private Integer beverageAmount = null;
    private Integer alcoholAmount = null;
    private Integer sleepMinutes = null;
    private Integer exerciseMinutes = null;
    private Integer exerciseSteps = null;
    
    private int successCount;         // 성공한 항목 수
    private int totalCount = 7;       // 전체 항목 수
    
    public DailyReport(String date) {
        this.date = date;
    }
    
    public void calculateSuccessCount() {
        successCount = 0;
        if (Boolean.TRUE.equals(proteinSuccess)) successCount++;
        if (Boolean.TRUE.equals(sodiumSuccess)) successCount++;
        if (Boolean.TRUE.equals(waterSuccess)) successCount++;
        if (Boolean.TRUE.equals(beverageSuccess)) successCount++;
        if (Boolean.TRUE.equals(alcoholSuccess)) successCount++;
        if (Boolean.TRUE.equals(sleepSuccess)) successCount++;
        if (Boolean.TRUE.equals(exerciseSuccess)) successCount++;
    }
    
    public int getSuccessRate() {
        return (int) ((successCount * 100.0) / totalCount);
    }
    
    public String getGrade() {
        int rate = getSuccessRate();
        if (rate == 100) return "✓";
        else if (rate >= 50) return "△";
        else return "✗";
    }
    
    // Getters and Setters - Success
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    
    public Boolean isProteinSuccess() { return proteinSuccess; }
    public void setProteinSuccess(Boolean proteinSuccess) { this.proteinSuccess = proteinSuccess; calculateSuccessCount(); }
    
    public Boolean isSodiumSuccess() { return sodiumSuccess; }
    public void setSodiumSuccess(Boolean sodiumSuccess) { this.sodiumSuccess = sodiumSuccess; calculateSuccessCount(); }
    
    public Boolean isWaterSuccess() { return waterSuccess; }
    public void setWaterSuccess(Boolean waterSuccess) { this.waterSuccess = waterSuccess; calculateSuccessCount(); }
    
    public Boolean isBeverageSuccess() { return beverageSuccess; }
    public void setBeverageSuccess(Boolean beverageSuccess) { this.beverageSuccess = beverageSuccess; calculateSuccessCount(); }
    
    public Boolean isAlcoholSuccess() { return alcoholSuccess; }
    public void setAlcoholSuccess(Boolean alcoholSuccess) { this.alcoholSuccess = alcoholSuccess; calculateSuccessCount(); }
    
    public Boolean isSleepSuccess() { return sleepSuccess; }
    public void setSleepSuccess(Boolean sleepSuccess) { this.sleepSuccess = sleepSuccess; calculateSuccessCount(); }
    
    public Boolean isExerciseSuccess() { return exerciseSuccess; }
    public void setExerciseSuccess(Boolean exerciseSuccess) { this.exerciseSuccess = exerciseSuccess; calculateSuccessCount(); }
    
    public int getSuccessCount() { return successCount; }
    public int getTotalCount() { return totalCount; }

    // [추가] Getters and Setters - Amounts
    public Integer getProteinAmount() { return proteinAmount; }
    public void setProteinAmount(Integer proteinAmount) { this.proteinAmount = proteinAmount; }

    public Integer getSodiumAmount() { return sodiumAmount; }
    public void setSodiumAmount(Integer sodiumAmount) { this.sodiumAmount = sodiumAmount; }

    public Integer getWaterAmount() { return waterAmount; }
    public void setWaterAmount(Integer waterAmount) { this.waterAmount = waterAmount; }

    public Integer getBeverageAmount() { return beverageAmount; }
    public void setBeverageAmount(Integer beverageAmount) { this.beverageAmount = beverageAmount; }

    public Integer getAlcoholAmount() { return alcoholAmount; }
    public void setAlcoholAmount(Integer alcoholAmount) { this.alcoholAmount = alcoholAmount; }

    public Integer getSleepMinutes() { return sleepMinutes; }
    public void setSleepMinutes(Integer sleepMinutes) { this.sleepMinutes = sleepMinutes; }

    public Integer getExerciseMinutes() { return exerciseMinutes; }
    public void setExerciseMinutes(Integer exerciseMinutes) { this.exerciseMinutes = exerciseMinutes; }

    public Integer getExerciseSteps() { return exerciseSteps; }
    public void setExerciseSteps(Integer exerciseSteps) { this.exerciseSteps = exerciseSteps; }
}
