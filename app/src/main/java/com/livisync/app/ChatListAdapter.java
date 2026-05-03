package com.livisync.app;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ViewHolder> {

    public interface OnChatClick {
        void onClick(ChatItem item);
    }

    private List<ChatItem> list;
    private OnChatClick listener;

    public ChatListAdapter(List<ChatItem> list, OnChatClick listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatItem item = list.get(position);
        holder.tvName.setText(item.getOtherName());
        holder.tvLastMessage.setText(item.getLastMessage());

        String initials = String.valueOf(item.getOtherName().charAt(0)).toUpperCase();
        holder.tvAvatar.setText(initials);

        if (item.isUnread()) {
            holder.tvName.setTypeface(null, Typeface.BOLD);
            holder.tvLastMessage.setTypeface(null, Typeface.BOLD);
            holder.tvLastMessage.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.darkGrey));
        } else {
            holder.tvName.setTypeface(null, Typeface.NORMAL);
            holder.tvLastMessage.setTypeface(null, Typeface.NORMAL);
            holder.tvLastMessage.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.grey));
        }

        holder.itemView.setOnClickListener(v -> listener.onClick(item));
    }

    @Override
    public int getItemCount() { return list.size(); }

    public void updateList(List<ChatItem> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvLastMessage, tvAvatar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvChatName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvAvatar = itemView.findViewById(R.id.tvChatAvatar);
        }
    }
}