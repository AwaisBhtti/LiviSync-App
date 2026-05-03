package com.livisync.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminMatchAdapter extends RecyclerView.Adapter<AdminMatchAdapter.ViewHolder> {

    public interface OnMatchActionListener {
        void onDelete(AdminMatchItem match);
    }

    private List<AdminMatchItem> list;
    private OnMatchActionListener listener;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    public AdminMatchAdapter(List<AdminMatchItem> list, OnMatchActionListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_match, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AdminMatchItem item = list.get(position);
        holder.tvUser1Name.setText(item.getUser1Name());
        holder.tvUser1Email.setText(item.getUser1Email());
        holder.tvUser2Name.setText(item.getUser2Name());
        holder.tvUser2Email.setText(item.getUser2Email());
        holder.tvMatchDate.setText("Matched: " + sdf.format(new Date(item.getTimestamp())));

        holder.ivDelete.setOnClickListener(v -> listener.onDelete(item));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUser1Name, tvUser1Email, tvUser2Name, tvUser2Email, tvMatchDate;
        ImageView ivDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUser1Name = itemView.findViewById(R.id.tvUser1Name);
            tvUser1Email = itemView.findViewById(R.id.tvUser1Email);
            tvUser2Name = itemView.findViewById(R.id.tvUser2Name);
            tvUser2Email = itemView.findViewById(R.id.tvUser2Email);
            tvMatchDate = itemView.findViewById(R.id.tvMatchDate);
            ivDelete = itemView.findViewById(R.id.ivDeleteMatch);
        }
    }
}