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
    
    private FoodAdapter adapter;
    private List<FoodType> foodList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food);

        etFoodName = findViewById(R.id.et_food_name);
        lvFoodList = findViewById(R.id.lv_food_list);
        apiService = SupabaseClient.getApi(this);

        adapter = new FoodAdapter(this, foodList, new FoodAdapter.OnDeleteListener() {
            @Override
            public void onDelete(FoodType food) {
                showDeleteConfirmDialog(food);
            }
        });
        lvFoodList.setAdapter(adapter);

        findViewById(R.id.btn_add_food).setOnClickListener(v -> addFood());

        loadFoodList();
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showSuccess(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showDeleteConfirmDialog(FoodType food) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_delete_confirm, null);
        builder.setView(view);

        final AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvMessage = view.findViewById(R.id.tv_dialog_message);
        Button btnCancel = view.findViewById(R.id.btn_cancel);
        Button btnDelete = view.findViewById(R.id.btn_delete);

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
                } else {
                    showError("목록을 불러올 수 없습니다.");
                }
            }
            @Override
            public void onFailure(Call<List<FoodType>> call, Throwable t) {
                android.util.Log.e("FoodActivity", "API Error", t);
                showError("목록 로드 중 오류가 발생했습니다.");
            }
        });
    }

    private void addFood() {
        String name = etFoodName.getText().toString().trim();
        if (name.isEmpty()) {
            showError("음식 이름을 입력하세요.");
            return;
        }

        apiService.insertFoodType(new FoodType(name)).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    showSuccess("추가되었습니다.");
                    etFoodName.setText("");
                    loadFoodList();
                } else {
                    showError("추가 실패");
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                android.util.Log.e("FoodActivity", "API Error", t);
                showError("추가 중 오류가 발생했습니다.");
            }
        });
    }

    private void deleteFood(long id) {
        apiService.deleteFoodType("eq." + id).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    showSuccess("삭제되었습니다.");
                    loadFoodList();
                } else {
                    showError("삭제 실패");
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                android.util.Log.e("FoodActivity", "API Error", t);
                showError("삭제 중 오류가 발생했습니다.");
            }
        });
    }
}
