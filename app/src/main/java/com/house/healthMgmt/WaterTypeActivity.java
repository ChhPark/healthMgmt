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

public class WaterTypeActivity extends AppCompatActivity {

    private EditText etName;
    private Button btnAdd;
    private ListView lvList;
    private SupabaseApi apiService;

    // [수정] 검색 기능을 위해 리스트를 두 개로 분리
    private List<WaterType> originalList = new ArrayList<>(); // 서버에서 가져온 전체 원본
    private List<WaterType> filteredList = new ArrayList<>(); // 검색어에 맞게 걸러진 리스트 (화면 표시용)
    
    private TypeAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_water_type);

        etName = findViewById(R.id.et_type_name);
        btnAdd = findViewById(R.id.btn_add);
        lvList = findViewById(R.id.lv_type_list);
        apiService = SupabaseClient.getApi(this);

        // 어댑터는 filteredList를 바라보게 설정
        adapter = new TypeAdapter(this, filteredList);
        lvList.setAdapter(adapter);

        btnAdd.setOnClickListener(v -> addType());

        // [추가] 텍스트 입력 시 자동 검색 기능
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

    // [추가] 검색어에 따라 리스트 필터링
    private void filterList(String text) {
        filteredList.clear();
        if (text.isEmpty()) {
            filteredList.addAll(originalList);
        } else {
            for (WaterType item : originalList) {
                if (item.getName().contains(text)) {
                    filteredList.add(item);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void fetchTypes() {
        apiService.getWaterTypes().enqueue(new Callback<List<WaterType>>() {
            @Override
            public void onResponse(Call<List<WaterType>> call, Response<List<WaterType>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    originalList.clear();
                    List<WaterType> fetched = response.body();

                    // ID 역순(최신순) 정렬
                    Collections.sort(fetched, new Comparator<WaterType>() {
                        @Override
                        public int compare(WaterType o1, WaterType o2) {
                            return Long.compare(o2.getId(), o1.getId());
                        }
                    });

                    originalList.addAll(fetched);
                    
                    // 데이터를 받아온 후 현재 입력된 검색어로 필터링 적용
                    filterList(etName.getText().toString());
                }
            }
            @Override
            public void onFailure(Call<List<WaterType>> call, Throwable t) {}
        });
    }

    private void addType() {
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) return;

        // [추가] 중복 체크
        for (WaterType type : originalList) {
            if (type.getName().equals(name)) {
                Toast.makeText(this, "이미 존재하는 물 종류입니다.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        WaterType newType = new WaterType(name);
        apiService.insertWaterType(newType).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    etName.setText(""); // 입력창 초기화
                    fetchTypes(); // 목록 갱신
                    Toast.makeText(WaterTypeActivity.this, "추가됨", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {}
        });
    }

    private void deleteType(WaterType type) {
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
            apiService.deleteWaterType("eq." + type.getId()).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        fetchTypes();
                    }
                }
                @Override
                public void onFailure(Call<Void> call, Throwable t) {}
            });
            dialog.dismiss();
        });

        dialog.show();
    }

    // 선택한 물 이름을 반환하고 종료
    private void returnSelectedWater(String name) {
        Intent intent = new Intent();
        intent.putExtra("selected_water", name);
        setResult(RESULT_OK, intent);
        finish();
    }

    private class TypeAdapter extends ArrayAdapter<WaterType> {
        public TypeAdapter(Context context, List<WaterType> list) {
            super(context, 0, list);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_water_type, parent, false);
            }
            WaterType item = getItem(position);
            
            TextView tvName = convertView.findViewById(R.id.tv_name);
            tvName.setText(item.getName());

            // 아이템 클릭 시 선택 및 반환
            convertView.setOnClickListener(v -> returnSelectedWater(item.getName()));

            convertView.findViewById(R.id.iv_delete).setOnClickListener(v -> deleteType(item));
            return convertView;
        }
    }
}
