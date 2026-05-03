package com.livisync.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AdminMatchesFragment extends Fragment {

    private TextView tvMatchCount;
    private RecyclerView rvAdminMatches;
    private FirebaseFirestore db;
    private AdminMatchAdapter adapter;
    private List<AdminMatchItem> matchList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_matches, container, false);

        tvMatchCount = view.findViewById(R.id.tvMatchCount);
        rvAdminMatches = view.findViewById(R.id.rvAdminMatches);
        db = FirebaseFirestore.getInstance();

        rvAdminMatches.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AdminMatchAdapter(matchList, this::showDeleteConfirmation);
        rvAdminMatches.setAdapter(adapter);

        loadMatches();

        return view;
    }

    private void loadMatches() {
        db.collection("matches").addSnapshotListener((value, error) -> {
            if (error != null) {
                if (getContext() != null)
                    Toast.makeText(getContext(), "Error loading matches", Toast.LENGTH_SHORT).show();
                return;
            }

            if (value != null) {
                matchList.clear();
                List<DocumentSnapshot> matchDocs = value.getDocuments();
                if (matchDocs.isEmpty()) {
                    tvMatchCount.setText("Total Matches: 0");
                    adapter.notifyDataSetChanged();
                    return;
                }

                for (DocumentSnapshot matchDoc : matchDocs) {
                    String matchId = matchDoc.getId();
                    String user1Uid = matchDoc.getString("user1");
                    String user2Uid = matchDoc.getString("user2");
                    Long timestamp = matchDoc.getLong("timestamp");
                    if (timestamp == null) timestamp = 0L;

                    final long finalTimestamp = timestamp;

                    db.collection("users").document(user1Uid).get().addOnSuccessListener(user1Doc -> {
                        String user1Name = user1Doc.getString("name");
                        String user1Email = user1Doc.getString("email");

                        db.collection("users").document(user2Uid).get().addOnSuccessListener(user2Doc -> {
                            String user2Name = user2Doc.getString("name");
                            String user2Email = user2Doc.getString("email");

                            matchList.add(new AdminMatchItem(
                                    matchId,
                                    user1Uid, user1Name != null ? user1Name : "Unknown", user1Email != null ? user1Email : "",
                                    user2Uid, user2Name != null ? user2Name : "Unknown", user2Email != null ? user2Email : "",
                                    finalTimestamp
                            ));

                            tvMatchCount.setText("Total Matches: " + matchList.size());
                            adapter.notifyDataSetChanged();
                        });
                    });
                }
            }
        });
    }

    private void showDeleteConfirmation(AdminMatchItem match) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Match")
                .setMessage("Are you sure you want to delete the match between " + match.getUser1Name() + " and " + match.getUser2Name() + "?")
                .setPositiveButton("Delete", (dialog, which) -> deleteMatch(match))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteMatch(AdminMatchItem match) {
        db.collection("matches").document(match.getMatchId()).delete()
                .addOnSuccessListener(aVoid -> {
                    deleteMessagesForMatch(match.getMatchId());
                    if (getContext() != null)
                        Toast.makeText(getContext(), "Match deleted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null)
                        Toast.makeText(getContext(), "Failed to delete match", Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteMessagesForMatch(String matchId) {
        db.collection("messages").document(matchId).collection("chats").get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot doc : snap.getDocuments()) doc.getReference().delete();
                    db.collection("messages").document(matchId).delete();
                });
    }
}