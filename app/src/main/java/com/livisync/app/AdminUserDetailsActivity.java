package com.livisync.app;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

public class AdminUserDetailsActivity extends AppCompatActivity {

    private TextView tvName, tvEmail, tvRole, tvAge, tvGender, tvBio;
    private TextView tvCity, tvSleep, tvCleanliness, tvBudget, tvSmoking, tvPets, tvGuests;
    private Button btnSuspend, btnActivate, btnDelete;
    private FirebaseFirestore db;
    private String uid, userName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_user_details);

        uid = getIntent().getStringExtra("uid");
        db = FirebaseFirestore.getInstance();

        tvName = findViewById(R.id.tvDetailName);
        tvEmail = findViewById(R.id.tvDetailEmail);
        tvRole = findViewById(R.id.tvDetailRole);
        tvAge = findViewById(R.id.tvDetailAge);
        tvGender = findViewById(R.id.tvDetailGender);
        tvBio = findViewById(R.id.tvDetailBio);

        tvCity = findViewById(R.id.tvDetailCity);
        tvSleep = findViewById(R.id.tvDetailSleep);
        tvCleanliness = findViewById(R.id.tvDetailCleanliness);
        tvBudget = findViewById(R.id.tvDetailBudget);
        tvSmoking = findViewById(R.id.tvDetailSmoking);
        tvPets = findViewById(R.id.tvDetailPets);
        tvGuests = findViewById(R.id.tvDetailGuests);

        btnSuspend = findViewById(R.id.btnSuspendUser);
        btnActivate = findViewById(R.id.btnActivateUser);
        btnDelete = findViewById(R.id.btnDeleteUserCompletely);

        loadUserDetails();

        btnSuspend.setOnClickListener(v -> {
            btnSuspend.setBackgroundColor(getResources().getColor(R.color.grey));
            btnActivate.setBackgroundColor(getResources().getColor(R.color.black));
            updateRole("suspended");
        });
        btnActivate.setOnClickListener(v -> {
            btnActivate.setBackgroundColor(getResources().getColor(R.color.grey));
            btnSuspend.setBackgroundColor(getResources().getColor(R.color.black));
            updateRole("user");
        });
        btnDelete.setOnClickListener(v -> {
            showDeleteConfirmation();
            btnDelete.setBackgroundColor(getResources().getColor(R.color.grey));
        });
    }

    private void loadUserDetails() {
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                userName = doc.getString("name");
                tvName.setText(userName);
                tvEmail.setText(doc.getString("email"));
                String role = doc.getString("role");
                tvRole.setText("ROLE: " + (role != null ? role.toUpperCase() : "USER"));
                tvAge.setText("Age: " + doc.getString("age"));
                tvGender.setText("Gender: " + doc.getString("gender"));
                tvBio.setText("Bio: " + doc.getString("bio"));

                if ("suspended".equals(role)) {
                    btnSuspend.setVisibility(View.GONE);
                    btnActivate.setVisibility(View.VISIBLE);
                } else if ("admin".equals(role)) {
                    btnSuspend.setVisibility(View.GONE);
                    btnActivate.setVisibility(View.GONE);
                } else {
                    btnSuspend.setVisibility(View.VISIBLE);
                    btnActivate.setVisibility(View.GONE);
                }
            }
        });

        db.collection("preferences").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                tvCity.setText("City: " + doc.getString("city"));
                tvSleep.setText("Sleep: " + doc.getString("sleepSchedule"));
                tvCleanliness.setText("Cleanliness: " + doc.get("cleanliness") + "/5");
                tvBudget.setText("Budget: PKR " + doc.get("budgetMin") + " - " + doc.get("budgetMax"));
                tvSmoking.setText("Smoking: " + (Boolean.TRUE.equals(doc.getBoolean("smokingAllowed")) ? "Allowed" : "Not Allowed"));
                tvPets.setText("Pets: " + (Boolean.TRUE.equals(doc.getBoolean("petsAllowed")) ? "Allowed" : "Not Allowed"));
                tvGuests.setText("Guests: " + (Boolean.TRUE.equals(doc.getBoolean("guestsAllowed")) ? "Allowed" : "Not Allowed"));
            }
        });
    }

    private void updateRole(String newRole) {
        db.collection("users").document(uid).update("role", newRole)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "User " + (newRole.equals("suspended") ? "suspended" : "activated"), Toast.LENGTH_SHORT).show();
                    loadUserDetails();
                });
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete User")
                .setMessage("Are you sure you want to completely wipe " + userName + " and all their data? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteUserCompletely())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteUserCompletely() {
        WriteBatch batch = db.batch();
        batch.delete(db.collection("users").document(uid));
        batch.delete(db.collection("preferences").document(uid));

        batch.commit().addOnSuccessListener(aVoid -> {
            cleanupRelatedData(uid);
            Toast.makeText(this, "User deleted successfully", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void cleanupRelatedData(String uid) {
        deleteByField("matchRequests", "fromUid", uid);
        deleteByField("matchRequests", "toUid", uid);
        deleteByField("reports", "reporterUid", uid);
        deleteByField("reports", "reportedUid", uid);

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
        db.collection("messages").document(matchId).collection("chats").get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot doc : snap.getDocuments()) doc.getReference().delete();
                    db.collection("messages").document(matchId).delete();
                });
    }
}
