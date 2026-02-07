// com.house.healthMgmt/HealthItem.java
package com.house.healthMgmt;

public class HealthItem {
    private String title;
    private boolean isSuccess;
    private String colorCode; // 색상 코드 추가 (예: "#FF5722")

    public HealthItem(String title, boolean isSuccess, String colorCode) {
        this.title = title;
        this.isSuccess = isSuccess;
        this.colorCode = colorCode;
    }

    // getter 메서드들
    public String getTitle() { return title; }
    public boolean isSuccess() { return isSuccess; }
    public String getColorCode() { return colorCode; }
    
    // setter 필요시 추가
    public void setSuccess(boolean success) { isSuccess = success; }
}
