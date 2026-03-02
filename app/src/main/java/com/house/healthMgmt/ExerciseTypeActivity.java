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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ExerciseTypeActivity extends AppCompatActivity {

    private EditText etName;
    private Button btnAdd;
    private ListView lvList;
    private SupabaseApi apiService;

    // [검색/정렬] 리스트 분리
    private List<ExerciseType> originalList = new ArrayList<>();
    private List<ExerciseType> filteredList = new ArrayList<>();
    
    private TypeAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise_type);

        etName = findViewById(R.id.et_type_name);
        btnAdd = findViewById(R.id.btn_add);
        lvList = findViewById(R.id.lv_type_list);
        apiService = SupabaseClient.getApi(this);

        // 어댑터는 filteredList를 사용
        adapter = new TypeAdapter(this, filteredList);
        lvList.setAdapter(adapter);

        btnAdd.setOnClickListener(v -> addType());

        // [추가] 실시간 검색 기능
        etName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterList(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        fetchTypes();
    }

    // [검색] 필터링 로직
    private void filterList(String text) {
        filteredList.clear();
        if (text.isEmpty()) {
            filteredList.addAll(originalList);
        } else {
            for (ExerciseType item : originalList) {
                if (item.getName().contains(text)) {
                    filteredList.add(item);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void fetchTypes() {
        apiService.getExerciseTypes().enqueue(new Callback<List<ExerciseType>>() {
            @Override
            public void onResponse(Call<List<ExerciseType>> call, Response<List<ExerciseType>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    originalList.clear();
                    List<ExerciseType> fetched = response.body();

                    // [정렬] ID 역순 (최신순)
                    Collections.sort(fetched, new Comparator<ExerciseType>() {
                        @Override
                        public int compare(ExerciseType o1, ExerciseType o2) {
                            return Long.compare(o2.getId(), o1.getId());
                        }
                    });

                    originalList.addAll(fetched);
                    // 현재 입력된 검색어로 리스트 갱신
                    filterList(etName.getText().toString());
                }
            }
            @Override
            public void onFailure(Call<List<ExerciseType>> call, Throwable t) {}
        });
    }

    private void addType() {
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) return;

        // [중복 체크]
        for (ExerciseType type : originalList) {
            if (type.getName().equals(name)) {
                Toast.makeText(this, "이미 존재하는 운동입니다.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        ExerciseType newType = new ExerciseType(name);
        apiService.insertExerciseType(newType).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    etName.setText("");
                    fetchTypes();
                    Toast.makeText(ExerciseTypeActivity.this, "추가됨", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {}
        });
    }

    // [반환] 선택 후 종료 로직
    private void returnSelectedType(String name) {
        Intent intent = new Intent();
        intent.putExtra("selected_exercise_type", name);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void deleteType(ExerciseType type) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_delete_confirm, null);
        builder.setView(view);
        
        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvMessage = view.findViewById(R.id.tv_dialog_message);
        tvMessage.setText(type.getName() + " 삭제?");

        view.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btn_delete).setOnClickListener(v -> {
            apiService.deleteExerciseType("eq." + type.getId()).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) fetchTypes();
                }
                @Override
                public void onFailure(Call<Void> call, Throwable t) {}
            });
            dialog.dismiss();
        });

        dialog.show();
    }

    private class TypeAdapter extends ArrayAdapter<ExerciseType> {
        public TypeAdapter(Context context, List<ExerciseType> list) {
            super(context, 0, list);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_exercise_type, parent, false);
            }
            ExerciseType item = getItem(position);
            
            TextView tvName = convertView.findViewById(R.id.tv_name);
            tvName.setText(item.getName());

            // [추가] 아이템 클릭 시 선택 반환
            convertView.setOnClickListener(v -> returnSelectedType(item.getName()));

            convertView.findViewById(R.id.iv_delete).setOnClickListener(v -> deleteType(item));
            return convertView;
        }
    }
}
