package com.house.healthMgmt;

import com.google.gson.annotations.SerializedName;

public class AlcoholLog {
    @SerializedName("id")
    private Long id;

    @SerializedName("record_date")
    private String recordDate;

    @SerializedName("alcohol_type")
    private String alcoholType;

    @SerializedName("amount")
    private int amount;

    @SerializedName("user_id")
    private String userId;

    public AlcoholLog(String recordDate, String alcoholType, int amount, String userId) {
        this.recordDate = recordDate;
        this.alcoholType = alcoholType;
        this.amount = amount;
        this.userId = userId;
    }

    public Long getId() { return id; }
    public String getRecordDate() { return recordDate; }
    public String getAlcoholType() { return alcoholType; }
    public int getAmount() { return amount; }
    public String getUserId() { return userId; }
}
