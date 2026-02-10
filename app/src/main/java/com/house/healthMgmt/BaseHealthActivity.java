package com.house.healthMgmt;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 모든 건강관리 Activity의 기본 클래스
 * - Null 체크 로직 통합
 * - 네트워크 상태 확인
 * - 사용자 ID 관리
 * - 공통 기능 제공
 * - 코드 중복 제거
 */
public abstract class BaseHealthActivity extends AppCompatActivity {
    
    protected SupabaseApi apiService;
    protected String targetDate;
    protected String currentUserId; // 하드코딩 제거, 동적 할당
    
    private static final SimpleDateFormat DATE_FORMAT = 
        new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
    
    /**
     * onCreate에서 호출하여 userId 초기화
     */
    protected void initializeUserId() {
        currentUserId = UserIdManager.getInstance().getUserId(this);
    }
    
    /**
     * Intent에서 안전하게 날짜 가져오기
     */
    protected String getTargetDateFromIntent() {
        Intent intent = getIntent();
        
        if (intent == null) {
            return getTodayDate();
        }
        
        String date = intent.getStringExtra("target_date");
        
        if (date == null || date.trim().isEmpty()) {
            return getTodayDate();
        }
        
        return date;
    }
    
    /**
     * 오늘 날짜 반환 (yyyy-MM-dd)
     */
    protected String getTodayDate() {
        return DATE_FORMAT.format(new Date());
    }
    
    /**
     * 네트워크 연결 상태 확인
     */
    protected boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) 
            getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
        return false;
    }
    
    /**
     * 네트워크 연결 확인 후 작업 수행
     * @return true이면 네트워크 사용 가능, false이면 불가
     */
    protected boolean checkNetworkAndProceed() {
        if (!isNetworkAvailable()) {
            showError("인터넷 연결을 확인해주세요.");
            return false;
        }
        return true;
    }
    
    /**
     * 에러 메시지 표시
     */
    protected void showError(String message) {
        runOnUiThread(() -> 
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        );
    }
    
    /**
     * 성공 메시지 표시
     */
    protected void showSuccess(String message) {
        runOnUiThread(() -> 
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        );
    }
    
    /**
     * API 실패 공통 처리
     */
    protected void handleApiFailure(Throwable t) {
        android.util.Log.e(getClass().getSimpleName(), "API Error", t);
        
        // 네트워크 오류인지 확인
        if (!isNetworkAvailable()) {
            showError("인터넷 연결이 끊어졌습니다.");
        } else {
            showError("데이터 로딩 중 오류가 발생했습니다.");
        }
    }
}
