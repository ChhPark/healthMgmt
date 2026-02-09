package com.house.healthMgmt;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ExerciseTargetActivity extends AppCompatActivity {

    private EditText etTarget;
    private Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise_target);

        etTarget = findViewById(R.id.et_target);
        btnSave = findViewById(R.id.btn_save);

        // 저장된 목표값 불러오기 (기본값 30분)
        SharedPreferences prefs = getSharedPreferences("HealthPrefs", Context.MODE_PRIVATE);
        int savedTarget = prefs.getInt("exercise_target", 30);
        etTarget.setText(String.valueOf(savedTarget));

        // 빠른 선택 버튼 (30분, 60분, 90분)
        findViewById(R.id.btn_30).setOnClickListener(v -> etTarget.setText("30"));
        findViewById(R.id.btn_60).setOnClickListener(v -> etTarget.setText("60"));
        findViewById(R.id.btn_90).setOnClickListener(v -> etTarget.setText("90"));

        // 저장 버튼
        btnSave.setOnClickListener(v -> {
            String valStr = etTarget.getText().toString();
            if (valStr.isEmpty()) {
                Toast.makeText(this, "목표 시간을 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            int target = Integer.parseInt(valStr);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("exercise_target", target);
            editor.apply();

            Toast.makeText(this, "목표가 저장되었습니다.", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
