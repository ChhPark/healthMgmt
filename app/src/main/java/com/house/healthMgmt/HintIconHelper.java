package com.house.healthMgmt;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;

/**
 * 힌트 아이콘 헬퍼 클래스
 * - 숨겨진 기능을 알려주는 작은 힌트 아이콘 표시
 * - 아이콘 클릭 시 기능 설명 표시
 */
public class HintIconHelper {
    
    /**
     * TextView에 힌트 아이콘 추가
     * @param textView 힌트를 추가할 TextView
     * @param hintMessage 표시할 힌트 메시지
     */
    public static void addHintIcon(TextView textView, String hintMessage) {
        if (textView == null) return;
        
        // 기존 텍스트 뒤에 작은 정보 아이콘 추가
        String originalText = textView.getText().toString();
        textView.setText(originalText + " ⓘ");
        
        // 아이콘 클릭 시 힌트 표시
        textView.setOnClickListener(v -> showHintToast(v.getContext(), hintMessage));
    }
    
    /**
     * TextView에 힌트 아이콘 추가 (기존 클릭 리스너 유지)
     * @param textView 힌트를 추가할 TextView
     * @param hintMessage 표시할 힌트 메시지
     * @param originalClickListener 기존 클릭 리스너
     */
    public static void addHintIconWithListener(TextView textView, String hintMessage, View.OnClickListener originalClickListener) {
        if (textView == null) return;
        
        // 기존 텍스트 뒤에 작은 정보 아이콘 추가
        String originalText = textView.getText().toString();
        textView.setText(originalText + " ⓘ");
        
        // 더블 기능: 짧게 클릭 = 원래 기능, 길게 클릭 = 힌트 표시
        textView.setOnClickListener(originalClickListener);
        textView.setOnLongClickListener(v -> {
            showHintDialog(v.getContext(), hintMessage);
            return true;
        });
    }
    
    /**
     * View 옆에 작은 힌트 아이콘 추가하는 Helper TextView 생성
     * @param context Context
     * @param hintMessage 표시할 힌트 메시지
     * @return 힌트 아이콘 TextView
     */
    public static TextView createHintIconView(Context context, String hintMessage) {
        TextView hintIcon = new TextView(context);
        hintIcon.setText(" ⓘ");
        hintIcon.setTextSize(14);
        hintIcon.setTextColor(Color.parseColor("#999999")); // 회색
        hintIcon.setGravity(Gravity.CENTER);
        hintIcon.setPadding(8, 0, 8, 0);
        
        hintIcon.setOnClickListener(v -> showHintDialog(context, hintMessage));
        
        return hintIcon;
    }
    
    /**
     * Toast로 힌트 메시지 표시
     */
    private static void showHintToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
    
    /**
     * AlertDialog로 힌트 메시지 표시 (더 자세한 설명용)
     */
    private static void showHintDialog(Context context, String message) {
        new AlertDialog.Builder(context)
            .setTitle("💡 사용 팁")
            .setMessage(message)
            .setPositiveButton("확인", null)
            .show();
    }
    
    /**
     * 힌트 메시지 상수들
     */
    public static class HintMessages {
        public static final String LONG_CLICK_CALENDAR = 
            "📅 날짜를 길게 누르면 달력이 나타나서\n원하는 날짜의 기록을 볼 수 있습니다.";
        
        public static final String CLICK_GOAL_SETTING = 
            "🎯 목표를 클릭하면 목표 설정 화면으로\n이동하여 목표값을 변경할 수 있습니다.";
        
        public static final String LONG_CLICK_MANAGE_ITEMS = 
            "📝 항목을 길게 누르면 관리 화면으로\n이동하여 항목을 추가하거나 삭제할 수 있습니다.";
        
        public static final String CLICK_WEIGHT_SETTING = 
            "⚖️ 체중 목표를 클릭하면 체중 기록 화면으로\n이동하여 현재 체중을 입력할 수 있습니다.";
    }
}
