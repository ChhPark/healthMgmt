package com.house.healthMgmt;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.List;

public class WeightAdapter extends ArrayAdapter<WeightLog> {
    public WeightAdapter(@NonNull Context context, List<WeightLog> logs) {
        super(context, 0, logs);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_weight, parent, false);
        }

        WeightLog log = getItem(position);
        if (log != null) {
            TextView tvDate = convertView.findViewById(R.id.tv_date);
            TextView tvWeight = convertView.findViewById(R.id.tv_weight);

            tvDate.setText(log.getRecordDate());
            tvWeight.setText(log.getWeight() + " kg");
        }

        return convertView;
    }
}
