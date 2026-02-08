package com.house.healthMgmt;

import com.google.gson.annotations.SerializedName;

public class BeverageType {
    @SerializedName("id")
    private Long id;

    @SerializedName("name")
    private String name;

    public BeverageType(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    // 스피너에서 객체 자체를 표시할 때 호출되는 메서드
    @Override
    public String toString() {
        return name;
    }
}
