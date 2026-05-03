package com.livisync.app;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MyReportAdapter extends RecyclerView.Adapter<MyReportAdapter.ViewHolder> {

    private List<AdminReportItem> list;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    public MyReportAdapter(List<AdminReportItem> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_my_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AdminReportItem item = list.get(position);
        holder.tvReportedName.setText(item.getReportedName());
        holder.tvReportedEmail.setText(item.getReportedEmail());
        holder.tvReason.setText(item.getReason());
        holder.tvDate.setText("Date: " + sdf.format(new Date(item.getTimestamp())));

        String status = item.getStatus() != null ? item.getStatus().toUpperCase() : "PENDING";
        holder.tvStatus.setText(status);

        if (status.equals("PENDING")) {
            holder.tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FFA500"))); // Orange
        } else if (status.equals("RESOLVED") || status.equals("DISMISSED")) {
            holder.tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50"))); // Green
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvReportedName, tvReportedEmail, tvReason, tvDate, tvStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvReportedName = itemView.findViewById(R.id.tvReportedName);
            tvReportedEmail = itemView.findViewById(R.id.tvReportedEmail);
            tvReason = itemView.findViewById(R.id.tvReportReason);
            tvDate = itemView.findViewById(R.id.tvReportDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}
