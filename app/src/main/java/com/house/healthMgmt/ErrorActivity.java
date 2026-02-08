package com.house.healthMgmt;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ErrorActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_error);

        TextView tvError = findViewById(R.id.tv_error_log);
        Button btnCopy = findViewById(R.id.btn_copy);

        // 전달받은 에러 내용 표시
        String errorLog = getIntent().getStringExtra("error_log");
        tvError.setText(errorLog);

        // 복사 버튼 기능
        btnCopy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Error Log", errorLog);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "클립보드에 복사되었습니다.", Toast.LENGTH_SHORT).show();
        });
    }
}
