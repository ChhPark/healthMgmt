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

    // 클릭 이벤트를 전달할 인터페이스 정의
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
        // 레이아웃이 없으면 생성
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_protein_record, parent, false);
        }

        // 현재 데이터 가져오기
        ProteinLog log = logs.get(position);

        // 뷰 연결
        TextView tvInfo = convertView.findViewById(R.id.tv_record_info);
        ImageView ivEdit = convertView.findViewById(R.id.iv_edit);
        ImageView ivDelete = convertView.findViewById(R.id.iv_delete);

        // 데이터 표시
        tvInfo.setText(log.getFoodType() + " : " + log.getProteinAmount() + "g");

        // 수정 버튼 클릭 시
        ivEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(log);
        });

        // 삭제 버튼 클릭 시
        ivDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(log);
        });

        return convertView;
    }
}
