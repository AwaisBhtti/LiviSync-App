package com.livisync.app;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ViewProfileActivity extends AppCompatActivity {

    TextView tvAvatar, tvName, tvMatchScore, tvAgeGender, tvBio;
    TextView tvCity, tvSleep, tvBudget, tvCleanliness, tvSmoking, tvPets, tvGuests;
    Button btnSendRequest, btnBack;

    FirebaseFirestore db;
    String myUid, theirUid;
    int matchScore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_profile);

        db = FirebaseFirestore.getInstance();
        myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        theirUid = getIntent().getStringExtra("uid");
        matchScore = getIntent().getIntExtra("score", 0);

        tvAvatar = findViewById(R.id.tvAvatar);
        tvName = findViewById(R.id.tvName);
        tvMatchScore = findViewById(R.id.tvMatchScore);
        tvAgeGender = findViewById(R.id.tvAgeGender);
        tvBio = findViewById(R.id.tvBio);
        tvCity = findViewById(R.id.tvCity);
        tvSleep = findViewById(R.id.tvSleep);
        tvBudget = findViewById(R.id.tvBudget);
        tvCleanliness = findViewById(R.id.tvCleanliness);
        tvSmoking = findViewById(R.id.tvSmoking);
        tvPets = findViewById(R.id.tvPets);
        tvGuests = findViewById(R.id.tvGuests);
        btnSendRequest = findViewById(R.id.btnSendRequest);
        btnBack = findViewById(R.id.btnBack);

        tvMatchScore.setText(matchScore + "% Match");

        loadProfile();

        btnBack.setOnClickListener(v -> finish());
        btnSendRequest.setOnClickListener(v -> sendRequest());
    }

    private void loadProfile() {
        // Load user info
        db.collection("users").document(theirUid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        String age = doc.getString("age");
                        String gender = doc.getString("gender");
                        String bio = doc.getString("bio");

                        tvName.setText(name);
                        tvAgeGender.setText(age + " • " + gender);
                        tvBio.setText(bio != null && !bio.isEmpty() ? bio : "No bio yet");

                        if (name != null && !name.isEmpty()) {
                            String initials = String.valueOf(name.charAt(0)).toUpperCase();
                            if (name.contains(" ")) {
                                initials += String.valueOf(name.split(" ")[1].charAt(0)).toUpperCase();
                            }
                            tvAvatar.setText(initials);
                        }
                    }
                });

        db.collection("preferences").document(theirUid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        tvCity.setText("Location: " + doc.getString("city"));
                        tvSleep.setText("Sleep: " + doc.getString("sleepSchedule"));
                        tvBudget.setText("Budget: PKR " + doc.getLong("budgetMin") + " - " + doc.getLong("budgetMax"));
                        tvCleanliness.setText("Cleanliness: " + doc.getLong("cleanliness") + "/5");
                        tvSmoking.setText("Smoking: " + (Boolean.TRUE.equals(doc.getBoolean("smokingAllowed")) ? "Allowed" : "Not Allowed"));
                        tvPets.setText("Pets: " + (Boolean.TRUE.equals(doc.getBoolean("petsAllowed")) ? "Allowed" : "Not Preferred"));
                        tvGuests.setText("Guests: " + (Boolean.TRUE.equals(doc.getBoolean("guestsAllowed")) ? "Allowed" : "Not Preferred"));
                    }
                });
    }

    private void sendRequest() {
        btnSendRequest.setEnabled(false);
        btnSendRequest.setBackgroundColor(getResources().getColor(R.color.grey));
        db.collection("matchRequests")
                .whereEqualTo("fromUid", myUid)
                .whereEqualTo("toUid", theirUid)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        Toast.makeText(this, "Request already sent", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Map<String, Object> request = new HashMap<>();
                    request.put("fromUid", myUid);
                    request.put("toUid", theirUid);
                    request.put("status", "pending");
                    request.put("timestamp", System.currentTimeMillis());

                    db.collection("matchRequests").add(request)
                            .addOnSuccessListener(ref -> {
                                Toast.makeText(this, "Match request sent!", Toast.LENGTH_SHORT).show();
                                btnSendRequest.setText("Request Sent");
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                btnSendRequest.setEnabled(true);
                                btnSendRequest.setBackgroundColor(getResources().getColor(R.color.darkGrey));
                            });
                });
    }
}