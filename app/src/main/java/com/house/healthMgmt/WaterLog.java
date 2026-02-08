package com.house.healthMgmt;

import com.google.gson.annotations.SerializedName;

public class WaterLog {
    @SerializedName("id")
    private Long id;

    @SerializedName("record_date")
    private String recordDate;

    @SerializedName("water_type") // food_type 대신 water_type 사용 (예: 생수, 보리차 등)
    private String waterType;

    @SerializedName("water_amount") // sodium_amount -> water_amount
    private int waterAmount;

    @SerializedName("user_id")
    private String userId;

    public WaterLog(String recordDate, String waterType, int waterAmount, String userId) {
        this.recordDate = recordDate;
        this.waterType = waterType;
        this.waterAmount = waterAmount;
        this.userId = userId;
    }

    public Long getId() { return id; }
    public String getRecordDate() { return recordDate; }
    public String getWaterType() { return waterType; }
    public int getWaterAmount() { return waterAmount; }
    public String getUserId() { return userId; }
}
