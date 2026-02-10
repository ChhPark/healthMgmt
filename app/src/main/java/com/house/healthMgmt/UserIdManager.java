package com.house.healthMgmt;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.UUID;

/**
 * 사용자 ID 관리 클래스
 * - SharedPreferences를 사용하여 고유 사용자 ID 관리
 * - 앱 최초 실행 시 UUID 자동 생성
 * - 멀티유저 지원 준비
 */
public class UserIdManager {
    
    private static final String PREFS_NAME = "HealthPrefs";
    private static final String KEY_USER_ID = "user_id";
    
    private static UserIdManager instance;
    private String userId;
    
    private UserIdManager() {
        // Singleton 패턴
    }
    
    /**
     * UserIdManager 인스턴스 가져오기
     */
    public static UserIdManager getInstance() {
        if (instance == null) {
            instance = new UserIdManager();
        }
        return instance;
    }
    
    /**
     * 사용자 ID 가져오기
     * - 최초 실행 시 UUID 생성 및 저장
     * - 이후에는 저장된 ID 반환
     */
    public String getUserId(Context context) {
        if (userId != null) {
            return userId;
        }
        
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        userId = prefs.getString(KEY_USER_ID, null);
        
        if (userId == null) {
            // 최초 실행: 새 UUID 생성
            userId = UUID.randomUUID().toString();
            prefs.edit().putString(KEY_USER_ID, userId).apply();
            android.util.Log.d("UserIdManager", "새 사용자 ID 생성: " + userId);
        } else {
            android.util.Log.d("UserIdManager", "기존 사용자 ID 사용: " + userId);
        }
        
        return userId;
    }
    
    /**
     * 사용자 ID 재설정 (테스트용 또는 초기화용)
     * 주의: 이 메서드를 호출하면 새로운 사용자로 간주됨
     */
    public void resetUserId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_USER_ID).apply();
        userId = null;
        android.util.Log.d("UserIdManager", "사용자 ID 초기화됨");
    }
    
    /**
     * 현재 사용자 ID 확인 (로그용)
     */
    public String getCurrentUserId() {
        return userId != null ? userId : "미설정";
    }
}
