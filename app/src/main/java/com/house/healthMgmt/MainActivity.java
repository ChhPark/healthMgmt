package com.house.healthMgmt;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import android.content.Intent;
import java.io.PrintWriter;
import java.io.StringWriter;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private HealthAdapter adapter;
    private List<HealthItem> healthItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		// --- [여기서부터 추가] 에러 감지 코드 시작 ---
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                // 1. 에러 내용을 문자열로 변환
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                throwable.printStackTrace(pw);
                String errorLog = sw.toString();

                // 2. ErrorActivity로 이동하면서 에러 내용 전달
                Intent intent = new Intent(MainActivity.this, ErrorActivity.class);
                intent.putExtra("error_log", errorLog);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);

                // 3. 현재 프로세스 강제 종료 (안 하면 앱이 멈춤)
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(1);
            }
        });
        // --- [여기까지 추가] 에러 감지 코드 끝 ---
		
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerView);
        
        // 2열 그리드 레이아웃 설정
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        // 데이터 초기화 (색상 코드 추가!)
        healthItems = new ArrayList<>();
        
        // 여기를 아래와 같이 색상 코드("#......")가 포함되도록 수정해야 합니다.
        healthItems.add(new HealthItem("단백질", false, "#009688")); // 청록
        healthItems.add(new HealthItem("나트륨", true, "#FF9800"));  // 주황
        healthItems.add(new HealthItem("물", false, "#2196F3"));     // 파랑
        healthItems.add(new HealthItem("No 음료수", true, "#F44336")); // 빨강
        healthItems.add(new HealthItem("No 술", true, "#9C27B0"));   // 보라
        healthItems.add(new HealthItem("수면", false, "#3F51B5"));   // 남색
        healthItems.add(new HealthItem("운동", true, "#4CAF50"));    // 초록

        // 어댑터 연결
        adapter = new HealthAdapter(healthItems);
        recyclerView.setAdapter(adapter);
    }
}
