package com.house.healthMgmt;
import com.google.gson.annotations.SerializedName;

public class AlcoholType {
    @SerializedName("id")
    private Long id;
    @SerializedName("name")
    private String name;

    public AlcoholType(String name) { this.name = name; }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String toString() { return name; }
}
