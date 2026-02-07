package com.house.healthMgmt;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private HealthAdapter adapter;
    private List<HealthItem> healthItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
