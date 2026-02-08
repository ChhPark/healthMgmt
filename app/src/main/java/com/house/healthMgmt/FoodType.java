package com.house.healthMgmt;

import com.google.gson.annotations.SerializedName;

public class FoodType {
    // [중요] long -> Long 으로 변경 (null 허용)
    // 값이 null이면 서버로 전송되지 않아, 서버가 자동으로 새 ID를 생성합니다.
    @SerializedName("id")
    private Long id;

    @SerializedName("name")
    private String name;

    // 생성자
    public FoodType(String name) {
        this.name = name;
    }

    // Getter
    public Long getId() { return id; }
    public String getName() { return name; }

    @Override
    public String toString() {
        return name;
    }
}
