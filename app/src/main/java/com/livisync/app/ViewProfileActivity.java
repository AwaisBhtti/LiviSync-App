package com.livisync.app;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ViewProfileActivity extends AppCompatActivity {

    TextView tvAvatar, tvName, tvMatchScore, tvAgeGender, tvBio;
    TextView tvCity, tvSleep, tvBudget, tvCleanliness, tvSmoking, tvPets, tvGuests, tvReport;
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
        tvReport = findViewById(R.id.tvReport);
        btnSendRequest = findViewById(R.id.btnSendRequest);
        btnBack = findViewById(R.id.btnBack);

        tvMatchScore.setText(matchScore + "% Match");

        loadProfile();
        checkInteractionStatus();

        btnBack.setOnClickListener(v -> finish());
        btnSendRequest.setOnClickListener(v -> sendRequest());
        tvReport.setOnClickListener(v -> showReportDialog());
    }

    private void showReportDialog() {
        String[] reasons = {"Inappropriate content", "Harassment", "Spam", "Fake Profile", "Other"};
        new AlertDialog.Builder(this)
                .setTitle("Report User")
                .setItems(reasons, (dialog, which) -> {
                    submitReport(reasons[which]);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void submitReport(String reason) {
        Map<String, Object> report = new HashMap<>();
        report.put("reporterUid", myUid);
        report.put("reportedUid", theirUid);
        report.put("reason", reason);
        report.put("timestamp", System.currentTimeMillis());
        report.put("status", "pending");

        db.collection("reports").add(report)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "Report submitted. Thank you.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to submit report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void checkInteractionStatus() {
        btnSendRequest.setEnabled(false);
        
        db.collection("matchRequests")
                .whereEqualTo("fromUid", myUid)
                .whereEqualTo("toUid", theirUid)
                .get()
                .addOnSuccessListener(snap1 -> {
                    if (!snap1.isEmpty()) {
                        updateButton("Request Sent", false);
                        return;
                    }
                    
                    db.collection("matchRequests")
                            .whereEqualTo("fromUid", theirUid)
                            .whereEqualTo("toUid", myUid)
                            .get()
                            .addOnSuccessListener(snap2 -> {
                                if (!snap2.isEmpty()) {
                                    updateButton("Request Pending", false);
                                    return;
                                }
                                
                                db.collection("matches")
                                        .whereEqualTo("user1", myUid)
                                        .whereEqualTo("user2", theirUid)
                                        .get()
                                        .addOnSuccessListener(snap3 -> {
                                            if (!snap3.isEmpty()) {
                                                updateButton("Friends", false);
                                                return;
                                            }
                                            
                                            db.collection("matches")
                                                    .whereEqualTo("user1", theirUid)
                                                    .whereEqualTo("user2", myUid)
                                                    .get()
                                                    .addOnSuccessListener(snap4 -> {
                                                        if (!snap4.isEmpty()) {
                                                            updateButton("Friends", false);
                                                        } else {
                                                            updateButton("Send Request", true);
                                                        }
                                                    });
                                        });
                            });
                });
    }

    private void updateButton(String text, boolean enabled) {
        btnSendRequest.setText(text);
        btnSendRequest.setEnabled(enabled);
        if (!enabled) {
            btnSendRequest.setBackgroundColor(getResources().getColor(R.color.grey));
        } else {
            btnSendRequest.setBackgroundColor(getResources().getColor(R.color.darkGrey));
        }
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
