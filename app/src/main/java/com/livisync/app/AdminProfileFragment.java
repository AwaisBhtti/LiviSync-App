package com.livisync.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminProfileFragment extends Fragment {

    private TextView tvAvatar, tvName, tvEmail;
    private Button btnLogout;
    private FirebaseFirestore db;
    private String uid;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_profile, container, false);

        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            uid = currentUser.getUid();
        }

        tvAvatar = view.findViewById(R.id.tvAdminAvatar);
        tvName = view.findViewById(R.id.tvAdminName);
        tvEmail = view.findViewById(R.id.tvAdminEmail);
        btnLogout = view.findViewById(R.id.btnAdminLogout);

        loadAdminProfile();

        btnLogout.setOnClickListener(v -> {
            btnLogout.setBackgroundColor(getResources().getColor(R.color.grey));
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(getActivity(), LoginActivity.class));
            requireActivity().finish();
        });

        return view;
    }

    private void loadAdminProfile() {
        if (uid == null) return;

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        String email = doc.getString("email");

                        if (name == null || name.isEmpty()) name = "Admin";
                        tvName.setText(name);
                        tvEmail.setText(email);

                        String initials = String.valueOf(name.charAt(0)).toUpperCase();
                        if (name.contains(" ")) {
                            initials += String.valueOf(name.split(" ")[1].charAt(0)).toUpperCase();
                        }
                        tvAvatar.setText(initials);
                    }
                });
    }
}