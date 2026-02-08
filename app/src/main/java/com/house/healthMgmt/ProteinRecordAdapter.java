package com.house.healthMgmt;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.List;

public class ProteinRecordAdapter extends ArrayAdapter<ProteinLog> {

    private Context context;
    private List<ProteinLog> logs;
    private OnRecordActionListener listener;

    public interface OnRecordActionListener {
        void onEdit(ProteinLog log);
        void onDelete(ProteinLog log);
    }

    public ProteinRecordAdapter(@NonNull Context context, List<ProteinLog> logs, OnRecordActionListener listener) {
        super(context, 0, logs);
        this.context = context;
        this.logs = logs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_protein_record, parent, false);
        }

        ProteinLog log = logs.get(position);

        // 1. 뷰 연결 (새로운 ID 사용)
        TextView tvFoodName = convertView.findViewById(R.id.tv_food_name);
        TextView tvAmount = convertView.findViewById(R.id.tv_protein_amt);
        ImageView ivEdit = convertView.findViewById(R.id.iv_edit);
        ImageView ivDelete = convertView.findViewById(R.id.iv_delete);

        // 2. 데이터 설정
        tvFoodName.setText(log.getFoodType()); // 음식 이름
        tvAmount.setText(String.valueOf(log.getProteinAmount())); // 숫자만 표시

        // 3. 버튼 이벤트
        ivEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(log);
        });

        ivDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(log);
        });

        return convertView;
    }
}
