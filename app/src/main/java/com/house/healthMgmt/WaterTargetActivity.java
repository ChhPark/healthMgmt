package com.house.healthMgmt;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class WaterTargetActivity extends AppCompatActivity {

    private EditText etTargetInput;
    private Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_water_target);

        etTargetInput = findViewById(R.id.et_target_input);
        btnSave = findViewById(R.id.btn_save_target);

        // 1. 기존 저장된 목표값 불러오기 (기본값 2000)
        SharedPreferences prefs = getSharedPreferences("HealthPrefs", Context.MODE_PRIVATE);
        int currentTarget = prefs.getInt("water_target", 2000);
        etTargetInput.setText(String.valueOf(currentTarget));
        etTargetInput.setSelection(etTargetInput.getText().length()); // 커서 끝으로 이동

        // 2. 저장 버튼 클릭
        btnSave.setOnClickListener(v -> {
            String inputStr = etTargetInput.getText().toString().trim();
            if (inputStr.isEmpty()) {
                Toast.makeText(this, "목표량을 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                int newTarget = Integer.parseInt(inputStr);
                
                // 목표값 저장 (SharedPreferences)
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt("water_target", newTarget);
                editor.apply();

                Toast.makeText(this, "목표가 수정되었습니다.", Toast.LENGTH_SHORT).show();
                finish(); // 화면 닫고 돌아가기
                
            } catch (NumberFormatException e) {
                Toast.makeText(this, "숫자만 입력 가능합니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
