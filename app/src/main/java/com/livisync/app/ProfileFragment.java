package com.livisync.app;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {

    TextView tvName, tvCityDept, tvSleep, tvCleanliness, tvBudget, tvGuests, tvPets, tvAvatar;
    Button btnEditProfile, btnLogout;
    FirebaseFirestore db;
    String uid;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            return view;
        }
        uid = currentUser.getUid();

        tvAvatar = view.findViewById(R.id.tvAvatar);
        tvName = view.findViewById(R.id.tvName);
        tvCityDept = view.findViewById(R.id.tvCityDept);
        tvSleep = view.findViewById(R.id.tvSleep);
        tvCleanliness = view.findViewById(R.id.tvCleanliness);
        tvBudget = view.findViewById(R.id.tvBudget);
        tvGuests = view.findViewById(R.id.tvGuests);
        tvPets = view.findViewById(R.id.tvPets);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnLogout = view.findViewById(R.id.btnLogout);

        loadProfile();

        btnEditProfile.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), EditProfileActivity.class));
        });

        btnLogout.setOnClickListener(v -> {
            btnLogout.setBackgroundColor(getResources().getColor(R.color.grey));
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(getActivity(), LoginActivity.class));
            requireActivity().finish();
        });

        return view;
    }

    private void loadProfile() {
        // Load user info
        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        String city = doc.getString("city") != null ? doc.getString("city") : "";

                        tvName.setText(name);
                        tvCityDept.setText("CS Student" + city);

                        if (name != null && !name.isEmpty()) {
                            String initials = String.valueOf(name.charAt(0)).toUpperCase();
                            if (name.contains(" ")) {
                                initials += String.valueOf(name.split(" ")[1].charAt(0)).toUpperCase();
                            }
                            tvAvatar.setText(initials);
                        }
                    }
                });

        // Load preferences
        db.collection("preferences").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        tvSleep.setText("Sleep: " + doc.getString("sleepSchedule"));
                        tvCleanliness.setText("Cleanliness: " + doc.get("cleanliness") + "/5");

                        Long min = doc.getLong("budgetMin");
                        Long max = doc.getLong("budgetMax");
                        tvBudget.setText("Budget: PKR " + min + " - " + max);

                        boolean guests = Boolean.TRUE.equals(doc.getBoolean("guestsAllowed"));
                        boolean pets = Boolean.TRUE.equals(doc.getBoolean("petsAllowed"));
                        tvGuests.setText("Guests: " + (guests ? "Allowed" : "Not Preferred"));
                        tvPets.setText("Pets: " + (pets ? "Allowed" : "Not Preferred"));
                    }
                });
    }



    @Override
    public void onResume() {
        super.onResume();
        loadProfile();
    }
}