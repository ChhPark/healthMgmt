package com.house.healthMgmt;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView; // [추가] ImageView
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.Collections; // [추가] 정렬을 위해 필요
    import java.util.Comparator;  // [추가] 정렬 기준 정의를 위해 필요


public class FoodActivity extends AppCompatActivity {

    private EditText etInput;
    private ListView lvFoodList;
    private Button btnAdd;

    private SupabaseApi apiService;
    
    // 전체 데이터와 검색된 데이터를 따로 관리
    private List<FoodType> originalList = new ArrayList<>();
    private List<FoodType> filteredList = new ArrayList<>();
    
    private FoodAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food);

        // [수정] XML ID에 맞춰 변경
        etInput = findViewById(R.id.et_food_name);     // et_food_input -> et_food_name
        btnAdd = findViewById(R.id.btn_add_food);      // btn_add -> btn_add_food
        lvFoodList = findViewById(R.id.lv_food_list);

        apiService = SupabaseClient.getApi(this);

        // 어댑터 초기화
        adapter = new FoodAdapter(this, filteredList, new FoodAdapter.OnItemActionListener() {
            @Override
            public void onDelete(FoodType food) {
                showDeleteConfirmDialog(food);
            }

            @Override
            public void onSelect(FoodType food) {
                returnSelectedFood(food.getName());
            }
        });
        lvFoodList.setAdapter(adapter);

        // 검색 기능
        etInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterList(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnAdd.setOnClickListener(v -> handleAddClick());
        
        fetchFoodTypes();
    }

    private void filterList(String text) {
        filteredList.clear();
        if (text.isEmpty()) {
            filteredList.addAll(originalList);
        } else {
            for (FoodType item : originalList) {
                if (item.getName().contains(text)) {
                    filteredList.add(item);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void returnSelectedFood(String foodName) {
        Intent intent = new Intent();
        intent.putExtra("selected_food", foodName);
        setResult(RESULT_OK, intent);
        finish();
    }

      

    private void fetchFoodTypes() {
        apiService.getFoodTypes().enqueue(new Callback<List<FoodType>>() {
            @Override
            public void onResponse(Call<List<FoodType>> call, Response<List<FoodType>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    originalList.clear();
                    originalList.addAll(response.body());

                    // [수정] ID 기준 내림차순 정렬 (최신 등록된 음식이 상단으로 오게 함)
                    // (ID가 클수록 나중에 생성된 데이터라고 가정)
                    Collections.sort(originalList, new Comparator<FoodType>() {
                        @Override
                        public int compare(FoodType o1, FoodType o2) {
                            // o2(뒤) - o1(앞) = 내림차순
                            // ID가 Long 타입일 경우 안전하게 compare 함수 사용
                            return Long.compare(o2.getId(), o1.getId());
                        }
                    });

                    // 정렬된 리스트를 기반으로 화면 갱신
                    filterList(etInput.getText().toString());
                }
            }
            @Override
            public void onFailure(Call<List<FoodType>> call, Throwable t) {}
        });
    }


    private void handleAddClick() {
        String inputStr = etInput.getText().toString();
        if (inputStr.isEmpty()) return;

        for (FoodType food : originalList) {
            if (food.getName().equals(inputStr)) {
                Toast.makeText(this, "이미 존재하는 음식입니다.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        FoodType newFood = new FoodType(inputStr);
        apiService.insertFoodType(newFood).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    etInput.setText("");
                    fetchFoodTypes();
                    Toast.makeText(FoodActivity.this, "추가됨", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {}
        });
    }

    private void showDeleteConfirmDialog(FoodType food) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // 다이얼로그 레이아웃은 기존 사용하던 것 유지 (dialog_delete_confirm)
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_delete_confirm, null);
        builder.setView(view);
        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        TextView tvMessage = view.findViewById(R.id.tv_dialog_message);
        view.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btn_delete).setOnClickListener(v -> {
            apiService.deleteFoodType("eq." + food.getId()).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) fetchFoodTypes();
                }
                @Override
                public void onFailure(Call<Void> call, Throwable t) {}
            });
            dialog.dismiss();
        });
        tvMessage.setText(food.getName() + " 삭제?");
        dialog.show();
    }

    private static class FoodAdapter extends ArrayAdapter<FoodType> {
        private Context context;
        private List<FoodType> list;
        private OnItemActionListener listener;

        public interface OnItemActionListener {
            void onDelete(FoodType food);
            void onSelect(FoodType food);
        }

        public FoodAdapter(@NonNull Context context, List<FoodType> list, OnItemActionListener listener) {
            super(context, 0, list);
            this.context = context;
            this.list = list;
            this.listener = listener;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                // [수정] item_food_manage -> item_food
                convertView = LayoutInflater.from(context).inflate(R.layout.item_food, parent, false);
            }

            FoodType food = list.get(position);
            
            // [수정] tv_item_name -> tv_food_name
            TextView tvName = convertView.findViewById(R.id.tv_food_name);
            View btnDelete = convertView.findViewById(R.id.iv_delete);

            tvName.setText(food.getName());

            convertView.setOnClickListener(v -> listener.onSelect(food));
            btnDelete.setOnClickListener(v -> listener.onDelete(food));

            return convertView;
        }
    }
}
