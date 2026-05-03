package com.livisync.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminReportAdapter extends RecyclerView.Adapter<AdminReportAdapter.ViewHolder> {

    public interface OnReportActionListener {
        void onDismiss(AdminReportItem report);
        void onResolve(AdminReportItem report);
        void onSuspend(AdminReportItem report);
    }

    private List<AdminReportItem> list;
    private OnReportActionListener listener;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    public AdminReportAdapter(List<AdminReportItem> list, OnReportActionListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AdminReportItem item = list.get(position);
        holder.tvReporterName.setText(item.getReporterName());
        holder.tvReporterEmail.setText(item.getReporterEmail());
        holder.tvReportedName.setText(item.getReportedName());
        holder.tvReportedEmail.setText(item.getReportedEmail());
        holder.tvReason.setText(item.getReason());
        holder.tvDate.setText("Date: " + sdf.format(new Date(item.getTimestamp())));

        holder.btnDismiss.setOnClickListener(v -> listener.onDismiss(item));
        holder.btnResolve.setOnClickListener(v -> listener.onResolve(item));
        holder.btnSuspend.setOnClickListener(v -> listener.onSuspend(item));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvReporterName, tvReporterEmail, tvReportedName, tvReportedEmail, tvReason, tvDate;
        Button btnDismiss, btnResolve, btnSuspend;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvReporterName = itemView.findViewById(R.id.tvReporterName);
            tvReporterEmail = itemView.findViewById(R.id.tvReporterEmail);
            tvReportedName = itemView.findViewById(R.id.tvReportedName);
            tvReportedEmail = itemView.findViewById(R.id.tvReportedEmail);
            tvReason = itemView.findViewById(R.id.tvReportReason);
            tvDate = itemView.findViewById(R.id.tvReportDate);
            btnDismiss = itemView.findViewById(R.id.btnDismiss);
            btnResolve = itemView.findViewById(R.id.btnResolve);
            btnSuspend = itemView.findViewById(R.id.btnSuspend);
        }
    }
}