package com.livisync.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RoommateAdapter extends RecyclerView.Adapter<RoommateAdapter.ViewHolder> {

    public interface OnActionListener {
        void onSendRequest(RoommateItem item);
        void onViewProfile(RoommateItem item);
    }

    private List<RoommateItem> list;
    private OnActionListener listener;

    public RoommateAdapter(List<RoommateItem> list, OnActionListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_roommate, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RoommateItem item = list.get(position);

        holder.tvName.setText(item.getName());
        holder.tvScore.setText(item.getScore() + "%");
        holder.tvBio.setText(item.getBio());
        
        String details = item.getCity() + " • " + item.getBudgetRange() + " • " + item.getSleep();
        holder.tvDetails.setText(details);

        holder.btnSend.setOnClickListener(v -> {
            listener.onSendRequest(item);
            holder.btnSend.setBackgroundColor(holder.itemView.getContext().getResources().getColor(R.color.grey));
            holder.btnSend.setEnabled(false);
        });
        holder.btnView.setOnClickListener(v -> listener.onViewProfile(item));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public void updateList(List<RoommateItem> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvScore, tvDetails, tvBio;
        Button btnSend, btnView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvRoomateName);
            tvScore = itemView.findViewById(R.id.tvMatchScore);
            tvDetails = itemView.findViewById(R.id.tvDetails);
            tvBio = itemView.findViewById(R.id.tvBio);
            btnSend = itemView.findViewById(R.id.btnSendRequest);
            btnView = itemView.findViewById(R.id.btnViewProfile);
        }
    }
}