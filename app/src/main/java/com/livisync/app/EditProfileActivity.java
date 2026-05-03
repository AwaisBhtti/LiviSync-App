package com.livisync.app;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private static final String TAG = "EditProfileActivity";

    EditText etName, etAge, etBio, etBudgetMin, etBudgetMax, etCity;
    Spinner spGender, spSleep, spCleanliness;
    SwitchMaterial swSmoking, swPets, swGuests;
    Button btnSaveChanges;
    FirebaseFirestore db;
    String uid;

    String[] genderOptions = {"Male", "Female"};
    String[] sleepOptions = {"Early Bird", "Late Night", "Night Owl", "Flexible"};
    String[] cleanOptions = {"1 - Very Relaxed", "2 - Relaxed", "3 - Average", "4 - Clean", "5 - Very Clean"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        uid = currentUser != null ? currentUser.getUid() : null;

        etName = findViewById(R.id.etName);
        etAge = findViewById(R.id.etAge);
        etBio = findViewById(R.id.etBio);
        etBudgetMin = findViewById(R.id.etBudgetMin);
        etBudgetMax = findViewById(R.id.etBudgetMax);
        etCity = findViewById(R.id.etCity);
        spGender = findViewById(R.id.spGender);
        spSleep = findViewById(R.id.spSleep);
        spCleanliness = findViewById(R.id.spCleanliness);
        swSmoking = findViewById(R.id.swSmoking);
        swPets = findViewById(R.id.swPets);
        swGuests = findViewById(R.id.swGuests);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);

        setupSpinners();
        loadCurrentData();

        btnSaveChanges.setOnClickListener(v -> saveChanges());
    }

    private void setupSpinners() {
        spGender.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, genderOptions));
        spSleep.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, sleepOptions));
        spCleanliness.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, cleanOptions));
    }

    private void loadCurrentData() {
        if (uid == null) return;

        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        etName.setText(doc.getString("name"));
                        etAge.setText(doc.getString("age"));
                        etBio.setText(doc.getString("bio"));

                        String gender = doc.getString("gender");
                        if (gender != null) {
                            for (int i = 0; i < genderOptions.length; i++) {
                                if (genderOptions[i].equals(gender)) {
                                    spGender.setSelection(i);
                                    break;
                                }
                            }
                        }
                    }
                });

        db.collection("preferences").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        etCity.setText(doc.getString("city"));

                        Long min = doc.getLong("budgetMin");
                        Long max = doc.getLong("budgetMax");
                        if (min != null) etBudgetMin.setText(String.valueOf(min));
                        if (max != null) etBudgetMax.setText(String.valueOf(max));

                        swSmoking.setChecked(Boolean.TRUE.equals(doc.getBoolean("smokingAllowed")));
                        swPets.setChecked(Boolean.TRUE.equals(doc.getBoolean("petsAllowed")));
                        swGuests.setChecked(Boolean.TRUE.equals(doc.getBoolean("guestsAllowed")));

                        String sleep = doc.getString("sleepSchedule");
                        if (sleep != null) {
                            for (int i = 0; i < sleepOptions.length; i++) {
                                if (sleepOptions[i].equals(sleep)) {
                                    spSleep.setSelection(i);
                                    break;
                                }
                            }
                        }

                        Long clean = doc.getLong("cleanliness");
                        if (clean != null) spCleanliness.setSelection((int)(clean - 1));
                    }
                });
    }

    private void saveChanges() {
        if (uid == null || uid.isEmpty()) {
            Toast.makeText(this, "You must be logged in to save changes.", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = etName.getText().toString().trim();
        String age = etAge.getText().toString().trim();
        String bio = etBio.getText().toString().trim();
        String city = etCity.getText().toString().trim();
        String budgetMin = etBudgetMin.getText().toString().trim();
        String budgetMax = etBudgetMax.getText().toString().trim();

        if (name.isEmpty()) {
            etName.setError("Required");
            return;
        }
        if (age.isEmpty()) {
            etAge.setError("Required");
            return;
        }
        if (city.isEmpty()) {
            etCity.setError("Required");
            return;
        }
        if (budgetMin.isEmpty()) {
            etBudgetMin.setError("Required");
            return;
        }
        if (budgetMax.isEmpty()) {
            etBudgetMax.setError("Required");
            return;
        }

        int parsedBudgetMin;
        int parsedBudgetMax;
        try {
            parsedBudgetMin = Integer.parseInt(budgetMin);
            parsedBudgetMax = Integer.parseInt(budgetMax);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Budget values must be numbers.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (parsedBudgetMin > parsedBudgetMax) {
            etBudgetMin.setError("Must be <= max");
            etBudgetMax.setError("Must be >= min");
            return;
        }

        Map<String, Object> userUpdates = new HashMap<>();
        userUpdates.put("name", name);
        userUpdates.put("age", age);
        userUpdates.put("bio", bio);
        userUpdates.put("gender", spGender.getSelectedItem().toString());

        Map<String, Object> prefUpdates = new HashMap<>();
        prefUpdates.put("sleepSchedule", spSleep.getSelectedItem().toString());
        prefUpdates.put("cleanliness", spCleanliness.getSelectedItemPosition() + 1);
        prefUpdates.put("budgetMin", parsedBudgetMin);
        prefUpdates.put("budgetMax", parsedBudgetMax);
        prefUpdates.put("city", city);
        prefUpdates.put("smokingAllowed", swSmoking.isChecked());
        prefUpdates.put("petsAllowed", swPets.isChecked());
        prefUpdates.put("guestsAllowed", swGuests.isChecked());

        WriteBatch batch = db.batch();
        batch.set(db.collection("users").document(uid), userUpdates, SetOptions.merge());
        batch.set(db.collection("preferences").document(uid), prefUpdates, SetOptions.merge());

        batch.commit()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();
                    finish(); // go back to profile fragment
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to save profile", e);
                });
    }
}
