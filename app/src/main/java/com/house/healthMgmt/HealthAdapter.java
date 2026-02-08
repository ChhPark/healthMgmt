// com.house.healthMgmt/HealthAdapter.java
package com.house.healthMgmt;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class HealthAdapter extends RecyclerView.Adapter<HealthAdapter.ViewHolder> {

    private List<HealthItem> itemList;

    public HealthAdapter(List<HealthItem> itemList) {
        this.itemList = itemList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_health_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HealthItem item = itemList.get(position);
        holder.tvTitle.setText(item.getTitle());

        // 1. O/X 텍스트 설정
        holder.tvStatus.setText(item.isSuccess() ? "O" : "X");

        // 2. 원형 배경 색상 동적 변경
        // bg_circle_icon.xml이 shape drawable이므로 GradientDrawable로 캐스팅해서 색을 바꿉니다.
        GradientDrawable bgShape = (GradientDrawable) holder.viewIconBg.getBackground();
        // 아이템에 지정된 색상 코드로 배경색 설정
        bgShape.setColor(Color.parseColor(item.getColorCode()));
		
	        // --- [추가된 부분 시작] ---
        // 카드 전체에 클릭 리스너 설정
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 항목 이름이 "단백질"이면 ProteinActivity로 이동
                if (item.getTitle().equals("단백질")) {
                    android.content.Context context = v.getContext();
                    android.content.Intent intent = new android.content.Intent(context, ProteinActivity.class);
                    context.startActivity(intent);
                }
                // 추후 다른 항목(물, 나트륨 등)도 여기서 else if로 추가하면 됩니다.
            }
        });
        // --- [추가된 부분 끝] ---
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvStatus;
        View viewIconBg; // 배경 원 View 추가

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvStatus = itemView.findViewById(R.id.tv_status);
            viewIconBg = itemView.findViewById(R.id.view_icon_bg);
        }
    }
}
