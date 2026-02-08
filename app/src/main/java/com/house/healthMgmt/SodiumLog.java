package com.house.healthMgmt;

import com.google.gson.annotations.SerializedName;

public class SodiumLog {
    @SerializedName("id")
    private Long id;

    @SerializedName("record_date")
    private String recordDate;

    @SerializedName("food_type")
    private String foodType;

    @SerializedName("sodium_amount") // 단백질(protein_amount) -> 나트륨(sodium_amount)
    private int sodiumAmount;

    @SerializedName("user_id")
    private String userId;

    public SodiumLog(String recordDate, String foodType, int sodiumAmount, String userId) {
        this.recordDate = recordDate;
        this.foodType = foodType;
        this.sodiumAmount = sodiumAmount;
        this.userId = userId;
    }

    public Long getId() { return id; }
    public String getRecordDate() { return recordDate; }
    public String getFoodType() { return foodType; }
    public int getSodiumAmount() { return sodiumAmount; }
    public String getUserId() { return userId; }
}
