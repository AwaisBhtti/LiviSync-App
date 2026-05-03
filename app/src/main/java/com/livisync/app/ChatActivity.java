package com.livisync.app;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    RecyclerView rvMessages;
    EditText etMessage;
    Button btnSend;
    TextView tvChatName, tvAvatar;

    FirebaseFirestore db;
    String myUid, matchId, otherUid, otherName;
    List<MessageItem> messageList = new ArrayList<>();
    MessageAdapter adapter;
    private ListenerRegistration matchListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        db = FirebaseFirestore.getInstance();
        myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        matchId = getIntent().getStringExtra("matchId");
        otherUid = getIntent().getStringExtra("otherUid");
        otherName = getIntent().getStringExtra("otherName");

        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        tvChatName = findViewById(R.id.tvChatName);
        tvAvatar = findViewById(R.id.tvChatAvatar);

        tvChatName.setText(otherName);

        String initials = String.valueOf(otherName.charAt(0)).toUpperCase();
        tvAvatar.setText(initials);

        adapter = new MessageAdapter(messageList, myUid);
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(adapter);

        listenForMessages();
        startMatchListener();

        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void startMatchListener() {
        // Clear unread flag whenever it becomes true while we are in this chat
        matchListener = db.collection("matches").document(matchId)
                .addSnapshotListener((doc, e) -> {
                    if (doc != null && doc.exists()) {
                        if (Boolean.TRUE.equals(doc.getBoolean("unread_" + myUid))) {
                            markAsRead();
                        }
                    }
                });
    }

    private void markAsRead() {
        db.collection("matches").document(matchId)
                .update("unread_" + myUid, false);
    }

    private void listenForMessages() {
        // Real time listener — updates instantly when new message arrives
        db.collection("messages")
                .document(matchId)
                .collection("chats")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (snap == null) return;
                    messageList.clear();
                    for (var doc : snap.getDocuments()) {
                        String text = doc.getString("text");
                        String sender = doc.getString("senderUid");
                        Long ts = doc.getLong("timestamp");
                        messageList.add(new MessageItem(text, sender, ts != null ? ts : 0));
                    }
                    adapter.updateList(messageList);
                    // Scroll to bottom
                    if (!messageList.isEmpty()) {
                        rvMessages.scrollToPosition(messageList.size() - 1);
                    }
                });
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        long timestamp = System.currentTimeMillis();

        Map<String, Object> message = new HashMap<>();
        message.put("text", text);
        message.put("senderUid", myUid);
        message.put("timestamp", timestamp);

        db.collection("messages")
                .document(matchId)
                .collection("chats")
                .add(message)
                .addOnSuccessListener(ref -> {
                    etMessage.setText("");
                    
                    Map<String, Object> update = new HashMap<>();
                    update.put("lastMessage", text);
                    update.put("lastMessageTimestamp", timestamp);
                    update.put("unread_" + otherUid, true);
                    
                    db.collection("matches").document(matchId).update(update);
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (matchListener != null) matchListener.remove();
    }
}