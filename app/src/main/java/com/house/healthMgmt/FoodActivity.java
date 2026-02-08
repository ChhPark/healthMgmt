package com.house.healthMgmt;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FoodActivity extends AppCompatActivity {
    private EditText etFoodName;
    private ListView lvFoodList;
    private SupabaseApi apiService;
    
    // 커스텀 어댑터 사용
    private FoodAdapter adapter;
    private List<FoodType> foodList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food);

        etFoodName = findViewById(R.id.et_food_name);
        lvFoodList = findViewById(R.id.lv_food_list);
        apiService = SupabaseClient.getApi(this);

        // 어댑터 연결 (삭제 버튼 클릭 리스너 포함)
        adapter = new FoodAdapter(this, foodList, new FoodAdapter.OnDeleteListener() {
            @Override
            public void onDelete(FoodType food) {
                showDeleteConfirmDialog(food); // 커스텀 팝업 호출
            }
        });
        lvFoodList.setAdapter(adapter);

        // 추가 버튼
        findViewById(R.id.btn_add_food).setOnClickListener(v -> addFood());

        // 초기 데이터 로드
        loadFoodList();
    }

    // --- [기능: 삭제 팝업 (Material Style)] ---
    private void showDeleteConfirmDialog(FoodType food) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        
        // 단백질 화면과 동일한 레이아웃 파일 재사용
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_delete_confirm, null);
        builder.setView(view);

        final AlertDialog dialog = builder.create();

        // 배경 투명 처리 (둥근 모서리 적용 필수)
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvMessage = view.findViewById(R.id.tv_dialog_message);
        Button btnCancel = view.findViewById(R.id.btn_cancel);
        Button btnDelete = view.findViewById(R.id.btn_delete);

        // 메시지 설정
        tvMessage.setText("'" + food.getName() + "' 항목을\n삭제하시겠습니까?");

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnDelete.setOnClickListener(v -> {
            deleteFood(food.getId());
            dialog.dismiss();
        });

        dialog.show();
    }

    private void loadFoodList() {
        apiService.getFoodTypes().enqueue(new Callback<List<FoodType>>() {
            @Override
            public void onResponse(Call<List<FoodType>> call, Response<List<FoodType>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    foodList.clear();
                    foodList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onFailure(Call<List<FoodType>> call, Throwable t) {
                Toast.makeText(FoodActivity.this, "목록 로드 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addFood() {
        String name = etFoodName.getText().toString().trim();
        if (name.isEmpty()) return;

        apiService.insertFoodType(new FoodType(name)).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    etFoodName.setText("");
                    loadFoodList();
                } else {
                    Toast.makeText(FoodActivity.this, "추가 실패", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(FoodActivity.this, "오류 발생", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteFood(long id) {
        apiService.deleteFoodType("eq." + id).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(FoodActivity.this, "삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    loadFoodList();
                } else {
                    Toast.makeText(FoodActivity.this, "삭제 실패", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(FoodActivity.this, "오류 발생", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
