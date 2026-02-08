package com.house.healthMgmt;

import com.google.gson.annotations.SerializedName;

public class ProteinLog {
    // [핵심 수정] long -> Long 으로 변경
    // 이렇게 해야 전송 시 id가 null이 되어, 서버에서 자동으로 1, 2, 3... 번호를 생성해줍니다.
    @SerializedName("id")
    private Long id;

    @SerializedName("record_date")
    private String recordDate;

    @SerializedName("food_type")
    private String foodType;

    @SerializedName("protein_amount")
    private int proteinAmount;

    @SerializedName("user_id")
    private String userId;

    // 생성자 (새 기록 추가용)
    public ProteinLog(String recordDate, String foodType, int proteinAmount, String userId) {
        this.recordDate = recordDate;
        this.foodType = foodType;
        this.proteinAmount = proteinAmount;
        this.userId = userId;
        // id는 초기화하지 않으므로 null 상태가 유지됨 -> 서버가 ID 자동 생성
    }

    // Getter
    public Long getId() { return id; }
    public String getRecordDate() { return recordDate; }
    public String getFoodType() { return foodType; }
    public int getProteinAmount() { return proteinAmount; }
    public String getUserId() { return userId; }
}
