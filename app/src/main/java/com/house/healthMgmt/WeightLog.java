package com.house.healthMgmt;
import com.google.gson.annotations.SerializedName;

public class WeightLog {
    @SerializedName("id") private Long id;
    @SerializedName("record_date") private String recordDate;
    @SerializedName("weight") private double weight;
    @SerializedName("user_id") private String userId;

    public WeightLog(String recordDate, double weight, String userId) {
        this.recordDate = recordDate;
        this.weight = weight;
        this.userId = userId;
    }

    public Long getId() { return id; }
    public double getWeight() { return weight; }
    // [추가됨] 날짜 Getter
    public String getRecordDate() { return recordDate; }
}
