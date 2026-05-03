package com.livisync.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ChatFragment extends Fragment {

    RecyclerView rvChats;
    FirebaseFirestore db;
    String myUid;
    List<ChatItem> chatList = new ArrayList<>();
    ChatListAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        db = FirebaseFirestore.getInstance();
        myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        rvChats = view.findViewById(R.id.rvChats);
        rvChats.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new ChatListAdapter(chatList, item -> {
            Intent intent = new Intent(getActivity(), ChatActivity.class);
            intent.putExtra("matchId", item.getMatchId());
            intent.putExtra("otherUid", item.getOtherUid());
            intent.putExtra("otherName", item.getOtherName());
            startActivity(intent);
        });
        rvChats.setAdapter(adapter);

        loadMatches();
        return view;
    }

    private void loadMatches() {
        // Use real-time listeners so unread status updates instantly
        db.collection("matches")
                .whereEqualTo("user1", myUid)
                .addSnapshotListener((snap, e) -> {
                    if (snap == null) return;
                    processMatchSnapshots(snap.getDocuments(), true);
                });

        db.collection("matches")
                .whereEqualTo("user2", myUid)
                .addSnapshotListener((snap, e) -> {
                    if (snap == null) return;
                    processMatchSnapshots(snap.getDocuments(), false);
                });
    }

    private synchronized void processMatchSnapshots(List<DocumentSnapshot> docs, boolean isUser1) {
        for (DocumentSnapshot doc : docs) {
            String matchId = doc.getId();
            String otherUid = isUser1 ? doc.getString("user2") : doc.getString("user1");
            boolean unread = Boolean.TRUE.equals(doc.getBoolean("unread_" + myUid));
            loadChatItem(matchId, otherUid, unread);
        }
    }

    private void loadChatItem(String matchId, String otherUid, boolean unread) {
        db.collection("users").document(otherUid).get()
                .addOnSuccessListener(userDoc -> {
                    String name = userDoc.getString("name");

                    // Get last message from the match document itself (more efficient)
                    db.collection("matches").document(matchId).get()
                            .addOnSuccessListener(matchDoc -> {
                                String lastMsg = matchDoc.getString("lastMessage");
                                if (lastMsg == null) lastMsg = "Say hello!";

                                ChatItem newItem = new ChatItem(matchId, otherUid, name, lastMsg, unread);
                                updateOrAddChatItem(newItem);
                            });
                });
    }

    private synchronized void updateOrAddChatItem(ChatItem newItem) {
        int index = -1;
        for (int i = 0; i < chatList.size(); i++) {
            if (chatList.get(i).getMatchId().equals(newItem.getMatchId())) {
                index = i;
                break;
            }
        }

        if (index != -1) {
            chatList.set(index, newItem);
        } else {
            chatList.add(newItem);
        }

        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> adapter.updateList(new ArrayList<>(chatList)));
        }
    }
}