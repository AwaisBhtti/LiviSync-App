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
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

public class AdminUsersFragment extends Fragment {

    private TextView tvUserCount;
    private RecyclerView rvAdminUsers;
    private FirebaseFirestore db;
    private AdminUserAdapter adapter;
    private List<UserItem> userList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_users, container, false);

        tvUserCount = view.findViewById(R.id.tvUserCount);
        rvAdminUsers = view.findViewById(R.id.rvAdminUsers);
        db = FirebaseFirestore.getInstance();

        rvAdminUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AdminUserAdapter(userList, this::showDeleteConfirmation);
        rvAdminUsers.setAdapter(adapter);

        loadUsers();

        return view;
    }

    private void loadUsers() {
        db.collection("users").addSnapshotListener((value, error) -> {
            if (error != null) {
                if (getContext() != null)
                    Toast.makeText(getContext(), "Error loading users", Toast.LENGTH_SHORT).show();
                return;
            }

            if (value != null) {
                userList.clear();
                for (DocumentSnapshot doc : value.getDocuments()) {
                    String uid = doc.getString("uid");
                    String name = doc.getString("name");
                    String email = doc.getString("email");
                    String role = doc.getString("role");
                    if (name == null || name.isEmpty()) name = "New User";
                    userList.add(new UserItem(uid, name, email, role));
                }
                tvUserCount.setText("Total Users: " + userList.size());
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void showDeleteConfirmation(UserItem user) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete User")
                .setMessage("Are you sure you want to completely wipe " + user.getName() + " and all their data? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteUserCompletely(user))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteUserCompletely(UserItem user) {
        String uid = user.getUid();
        WriteBatch batch = db.batch();

        batch.delete(db.collection("users").document(uid));
        batch.delete(db.collection("preferences").document(uid));

        batch.commit().addOnSuccessListener(aVoid -> {
            cleanupRelatedData(uid);
            if (getContext() != null)
                Toast.makeText(getContext(), "User profile and preferences deleted.", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            if (getContext() != null)
                Toast.makeText(getContext(), "Failed to delete profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void cleanupRelatedData(String uid) {
        // 2. Remove Match Requests (both ways)
        deleteByField("matchRequests", "fromUid", uid);
        deleteByField("matchRequests", "toUid", uid);

        // 3. Remove Reports (both ways)
        deleteByField("reports", "reporterUid", uid);
        deleteByField("reports", "reportedUid", uid);

        // 4. Remove Matches and Chat History
        db.collection("matches").whereEqualTo("user1", uid).get().addOnSuccessListener(snap -> {
            for (DocumentSnapshot doc : snap.getDocuments()) {
                deleteMessagesForMatch(doc.getId());
                doc.getReference().delete();
            }
        });
        db.collection("matches").whereEqualTo("user2", uid).get().addOnSuccessListener(snap -> {
            for (DocumentSnapshot doc : snap.getDocuments()) {
                deleteMessagesForMatch(doc.getId());
                doc.getReference().delete();
            }
        });
    }

    private void deleteByField(String collection, String field, String value) {
        db.collection(collection).whereEqualTo(field, value).get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot doc : snap.getDocuments()) doc.getReference().delete();
                });
    }

    private void deleteMessagesForMatch(String matchId) {
        // Wipes all messages in the 'chats' subcollection before deleting the 'messages' doc
        db.collection("messages").document(matchId).collection("chats").get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot doc : snap.getDocuments()) doc.getReference().delete();
                    db.collection("messages").document(matchId).delete();
                });
    }
}