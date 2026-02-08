package com.house.healthMgmt;
import com.google.gson.annotations.SerializedName;

public class DailySummary {
    @SerializedName("record_date")
    private String recordDate;
    
    @SerializedName("user_id")
    private String userId;
    
    @SerializedName("is_protein_success")
    private boolean isProteinSuccess;

    public DailySummary(String recordDate, String userId, boolean isProteinSuccess) {
        this.recordDate = recordDate;
        this.userId = userId;
        this.isProteinSuccess = isProteinSuccess;
    }
}
