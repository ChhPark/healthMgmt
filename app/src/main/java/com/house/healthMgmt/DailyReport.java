package com.house.healthMgmt;

/**
 * 일별 리포트 데이터 모델
 * 하루의 모든 건강 항목 성공/실패 정보를 담음
 */
public class DailyReport {
    
    private String date;              // 날짜 (yyyy-MM-dd)
    private boolean proteinSuccess;   // 단백질 목표 달성 여부
    private boolean sodiumSuccess;    // 나트륨 목표 달성 여부
    private boolean waterSuccess;     // 물 목표 달성 여부
    private boolean beverageSuccess;  // 음료수 목표 달성 여부 (0개)
    private boolean alcoholSuccess;   // 술 목표 달성 여부 (0개)
    private boolean sleepSuccess;     // 수면 목표 달성 여부
    private boolean exerciseSuccess;  // 운동 목표 달성 여부
    
    private int successCount;         // 성공한 항목 수
    private int totalCount = 7;       // 전체 항목 수
    
    public DailyReport(String date) {
        this.date = date;
    }
    
    /**
     * 성공한 항목 수 계산
     */
    public void calculateSuccessCount() {
        successCount = 0;
        if (proteinSuccess) successCount++;
        if (sodiumSuccess) successCount++;
        if (waterSuccess) successCount++;
        if (beverageSuccess) successCount++;
        if (alcoholSuccess) successCount++;
        if (sleepSuccess) successCount++;
        if (exerciseSuccess) successCount++;
    }
    
    /**
     * 성공률 계산 (0~100)
     */
    public int getSuccessRate() {
        return (int) ((successCount * 100.0) / totalCount);
    }
    
    /**
     * 성공 등급 반환
     * ✓ (우수): 100%
     * △ (보통): 50~99%
     * ✗ (미흡): 0~49%
     */
    public String getGrade() {
        int rate = getSuccessRate();
        if (rate == 100) return "✓";
        else if (rate >= 50) return "△";
        else return "✗";
    }
    
    // Getters and Setters
    
    public String getDate() {
        return date;
    }
    
    public void setDate(String date) {
        this.date = date;
    }
    
    public boolean isProteinSuccess() {
        return proteinSuccess;
    }
    
    public void setProteinSuccess(boolean proteinSuccess) {
        this.proteinSuccess = proteinSuccess;
        calculateSuccessCount();
    }
    
    public boolean isSodiumSuccess() {
        return sodiumSuccess;
    }
    
    public void setSodiumSuccess(boolean sodiumSuccess) {
        this.sodiumSuccess = sodiumSuccess;
        calculateSuccessCount();
    }
    
    public boolean isWaterSuccess() {
        return waterSuccess;
    }
    
    public void setWaterSuccess(boolean waterSuccess) {
        this.waterSuccess = waterSuccess;
        calculateSuccessCount();
    }
    
    public boolean isBeverageSuccess() {
        return beverageSuccess;
    }
    
    public void setBeverageSuccess(boolean beverageSuccess) {
        this.beverageSuccess = beverageSuccess;
        calculateSuccessCount();
    }
    
    public boolean isAlcoholSuccess() {
        return alcoholSuccess;
    }
    
    public void setAlcoholSuccess(boolean alcoholSuccess) {
        this.alcoholSuccess = alcoholSuccess;
        calculateSuccessCount();
    }
    
    public boolean isSleepSuccess() {
        return sleepSuccess;
    }
    
    public void setSleepSuccess(boolean sleepSuccess) {
        this.sleepSuccess = sleepSuccess;
        calculateSuccessCount();
    }
    
    public boolean isExerciseSuccess() {
        return exerciseSuccess;
    }
    
    public void setExerciseSuccess(boolean exerciseSuccess) {
        this.exerciseSuccess = exerciseSuccess;
        calculateSuccessCount();
    }
    
    public int getSuccessCount() {
        return successCount;
    }
    
    public int getTotalCount() {
        return totalCount;
    }
}
