package com.house.healthMgmt;

import com.google.gson.annotations.SerializedName;

public class SleepLog {
    @SerializedName("id")
    private Long id;

    @SerializedName("record_date")
    private String recordDate;

    @SerializedName("sleep_type")
    private String sleepType;

    @SerializedName("minutes") // 분 단위
    private int minutes;

    @SerializedName("user_id")
    private String userId;

    public SleepLog(String recordDate, String sleepType, int minutes, String userId) {
        this.recordDate = recordDate;
        this.sleepType = sleepType;
        this.minutes = minutes;
        this.userId = userId;
    }

    public Long getId() { return id; }
    public String getRecordDate() { return recordDate; }
    public String getSleepType() { return sleepType; }
    public int getMinutes() { return minutes; }
    public String getUserId() { return userId; }
}
