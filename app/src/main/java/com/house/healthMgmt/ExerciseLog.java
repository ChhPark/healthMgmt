package com.house.healthMgmt;
import com.google.gson.annotations.SerializedName;

public class ExerciseLog {
    @SerializedName("id") private Long id;
    @SerializedName("record_date") private String recordDate;
    @SerializedName("exercise_type") private String exerciseType;
    @SerializedName("minutes") private int minutes;
    @SerializedName("user_id") private String userId;

    public ExerciseLog(String recordDate, String exerciseType, int minutes, String userId) {
        this.recordDate = recordDate;
        this.exerciseType = exerciseType;
        this.minutes = minutes;
        this.userId = userId;
    }

    public Long getId() { return id; }
    public String getRecordDate() { return recordDate; }
    public String getExerciseType() { return exerciseType; }
    public int getMinutes() { return minutes; }
    public String getUserId() { return userId; }
}
