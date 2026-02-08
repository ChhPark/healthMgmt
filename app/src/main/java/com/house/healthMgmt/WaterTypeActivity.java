package com.house.healthMgmt;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
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
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WaterTypeActivity extends AppCompatActivity {

    private EditText etName;
    private Button btnAdd;
    private ListView lvList;
    private SupabaseApi apiService;
    private List<WaterType> typeList = new ArrayList<>();
    private TypeAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_water_type);

        etName = findViewById(R.id.et_type_name);
        btnAdd = findViewById(R.id.btn_add);
        lvList = findViewById(R.id.lv_type_list);
        apiService = SupabaseClient.getApi(this);

        adapter = new TypeAdapter(this, typeList);
        lvList.setAdapter(adapter);

        btnAdd.setOnClickListener(v -> addType());
        fetchTypes();
    }

    private void fetchTypes() {
        apiService.getWaterTypes().enqueue(new Callback<List<WaterType>>() {
            @Override
            public void onResponse(Call<List<WaterType>> call, Response<List<WaterType>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    typeList.clear();
                    typeList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onFailure(Call<List<WaterType>> call, Throwable t) {}
        });
    }

    private void addType() {
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) return;

        WaterType newType = new WaterType(name);
        apiService.insertWaterType(newType).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    etName.setText("");
                    fetchTypes();
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {}
        });
    }

    // [수정] 음식 관리 화면과 동일한 커스텀 삭제 팝업 적용
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

            convertView.findViewById(R.id.iv_delete).setOnClickListener(v -> deleteType(item));
            return convertView;
        }
    }
}
