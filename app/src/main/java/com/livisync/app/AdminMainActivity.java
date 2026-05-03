package com.livisync.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AdminMainActivity extends AppCompatActivity {

    private TextView tvUserCount;
    private RecyclerView rvAdminUsers;
    private Button btnLogout;
    private FirebaseFirestore db;
    private AdminUserAdapter adapter;
    private List<UserItem> userList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_main);

        tvUserCount = findViewById(R.id.tvUserCount);
        rvAdminUsers = findViewById(R.id.rvAdminUsers);
        btnLogout = findViewById(R.id.btnAdminLogout);
        db = FirebaseFirestore.getInstance();

        rvAdminUsers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminUserAdapter(userList, user -> {
            deleteUser(user);
        });
        rvAdminUsers.setAdapter(adapter);

        loadUsers();

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(AdminMainActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void loadUsers() {
        db.collection("users").addSnapshotListener((value, error) -> {
            if (error != null) {
                Toast.makeText(this, "Error loading users", Toast.LENGTH_SHORT).show();
                return;
            }

            if (value != null) {
                userList.clear();
                for (DocumentSnapshot doc : value.getDocuments()) {
                    String uid = doc.getString("uid");
                    String name = doc.getString("name");
                    String email = doc.getString("email");
                    String role = doc.getString("role");
                    
                    // Default name if empty
                    if (name == null || name.isEmpty()) name = "New User";
                    
                    userList.add(new UserItem(uid, name, email, role));
                }
                tvUserCount.setText("Total Users: " + userList.size());
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void deleteUser(UserItem user) {
        // Implementation for deleting user from Firestore
        // Note: Real Firebase Auth deletion requires Admin SDK or re-authentication
        db.collection("users").document(user.getUid()).delete()
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "User deleted from database", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete user", Toast.LENGTH_SHORT).show());
    }
}