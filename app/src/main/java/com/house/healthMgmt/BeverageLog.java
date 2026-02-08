package com.house.healthMgmt;

import com.google.gson.annotations.SerializedName;

public class BeverageLog {
    @SerializedName("id")
    private Long id;

    @SerializedName("record_date")
    private String recordDate;

    @SerializedName("beverage_type") // 음료 종류 (예: 콜라, 주스)
    private String beverageType;

    @SerializedName("amount") // 섭취량
    private int amount;

    @SerializedName("user_id")
    private String userId;

    public BeverageLog(String recordDate, String beverageType, int amount, String userId) {
        this.recordDate = recordDate;
        this.beverageType = beverageType;
        this.amount = amount;
        this.userId = userId;
    }

    public Long getId() { return id; }
    public String getRecordDate() { return recordDate; }
    public String getBeverageType() { return beverageType; }
    public int getAmount() { return amount; }
    public String getUserId() { return userId; }
}
