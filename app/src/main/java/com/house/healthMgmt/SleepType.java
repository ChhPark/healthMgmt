package com.house.healthMgmt;
import com.google.gson.annotations.SerializedName;

public class SleepType {
    @SerializedName("id")
    private Long id;
    @SerializedName("name")
    private String name;

    public SleepType(String name) { this.name = name; }
    public Long getId() { return id; }
    public String getName() { return name; }
    
    @Override
    public String toString() { return name; } // 스피너 표시용
}
