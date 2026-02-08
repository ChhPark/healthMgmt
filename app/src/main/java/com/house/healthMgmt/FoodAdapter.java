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

public class FoodAdapter extends ArrayAdapter<FoodType> {

    private Context context;
    private List<FoodType> foodList;
    private OnDeleteListener deleteListener;

    // 삭제 이벤트를 받아줄 인터페이스
    public interface OnDeleteListener {
        void onDelete(FoodType food);
    }

    public FoodAdapter(@NonNull Context context, List<FoodType> list, OnDeleteListener listener) {
        super(context, 0, list);
        this.context = context;
        this.foodList = list;
        this.deleteListener = listener;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_food, parent, false);
        }

        FoodType food = foodList.get(position);

        TextView tvName = convertView.findViewById(R.id.tv_food_name);
        ImageView ivDelete = convertView.findViewById(R.id.iv_delete);

        tvName.setText(food.getName());

        // 휴지통 아이콘 클릭 시
        ivDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDelete(food);
            }
        });

        return convertView;
    }
}
