package com.house.healthMgmt;
import com.google.gson.annotations.SerializedName;

public class ExerciseType {
    @SerializedName("id") private Long id;
    @SerializedName("name") private String name;

    public ExerciseType(String name) { this.name = name; }
    public Long getId() { return id; }
    public String getName() { return name; }
    
    @Override
    public String toString() { return name; }
}
