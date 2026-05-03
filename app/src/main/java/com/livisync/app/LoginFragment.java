package com.livisync.app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;


public class LoginFragment extends Fragment {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextInputEditText etEmail, etPassword;
    private Button btnLogin;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        btnLogin = view.findViewById(R.id.btnLogin);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnLogin.setOnClickListener(v -> {
            String inputEmail = etEmail.getText().toString().trim();
            String inputPassword = etPassword.getText().toString().trim();
            if (inputEmail.isEmpty() || inputPassword.isEmpty()) {
                Toast.makeText(getContext(), "Please fill out all fields.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(inputEmail).matches()) {
                etEmail.setError("Please enter a valid email");
                return;
            }
            if (inputPassword.length() < 6) {
                etPassword.setError("Password must be at least 8 characters");
                return;
            }

            btnLogin.setEnabled(false);
            btnLogin.setBackgroundColor(getResources().getColor(R.color.grey));
            
            mAuth.signInWithEmailAndPassword(inputEmail, inputPassword)
                    .addOnSuccessListener(authResult -> {
                        checkUserRole(authResult.getUser().getUid());
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Login failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        btnLogin.setEnabled(true);
                        btnLogin.setBackgroundColor(getResources().getColor(R.color.darkGrey));
                    });
        });
    }

    private void checkUserRole(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        if ("admin".equals(role)) {
                            startActivity(new Intent(getActivity(), AdminMainActivity.class));
                        } else {
                            startActivity(new Intent(getActivity(), MainActivity.class));
                        }
                        requireActivity().finish();
                    } else {
                        Toast.makeText(getContext(), "User data not found", Toast.LENGTH_SHORT).show();
                        btnLogin.setEnabled(true);
                        btnLogin.setBackgroundColor(getResources().getColor(R.color.darkGrey));
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnLogin.setEnabled(true);
                    btnLogin.setBackgroundColor(getResources().getColor(R.color.darkGrey));
                });
    }
}