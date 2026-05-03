package com.livisync.app;

import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import com.google.firebase.auth.FirebaseAuth;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignUpFragment extends Fragment {

    private TextInputEditText etEmail, etPassword, etVerifyPassword;
    private RadioGroup rgRole;
    private Button btnSignUp;
    FirebaseAuth mAuth;
    FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sign_up, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        etEmail = view.findViewById(R.id.etEmail2);
        etPassword = view.findViewById(R.id.etPassword2);
        etVerifyPassword = view.findViewById(R.id.etVerifyPassword);
        rgRole = view.findViewById(R.id.rgRole);
        btnSignUp = view.findViewById(R.id.btnSignUp);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnSignUp.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String verifyPassword = etVerifyPassword.getText().toString().trim();
            
            String role = "user";
            if (rgRole.getCheckedRadioButtonId() == R.id.rbAdmin) {
                role = "admin";
            }

            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Please enter a valid email");
                return;
            }
            if (password.length() < 8) {
                etPassword.setError("Password must be at least 8 characters");
                return;
            }
            if (!password.equals(verifyPassword)) {
                etVerifyPassword.setError("Passwords do not match");
                return;
            }
            
            btnSignUp.setEnabled(false);
            btnSignUp.setBackgroundColor(getResources().getColor(R.color.grey));
            
            final String selectedRole = role;
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        String uid = mAuth.getCurrentUser().getUid();
                        saveUserToFirestore(uid, email, selectedRole);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Registration failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        btnSignUp.setEnabled(true);
                        btnSignUp.setBackgroundColor(getResources().getColor(R.color.darkGrey));
                    });
        });
    }

    private void saveUserToFirestore(String uid, String email, String role) {
        Map<String, Object> user = new HashMap<>();
        user.put("uid", uid);
        user.put("name", "");
        user.put("email", email);
        user.put("role", role);
        user.put("age", "");
        user.put("bio", "");
        user.put("gender", "");
        user.put("photoUrl", "");

        db.collection("users").document(uid)
                .set(user)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(getContext(), "Registration Successful! Please Log In.", Toast.LENGTH_SHORT).show();
                    etEmail.setText("");
                    etPassword.setText("");
                    etVerifyPassword.setText("");
                    btnSignUp.setEnabled(true);
                    btnSignUp.setBackgroundColor(getResources().getColor(R.color.darkGrey));
                    
                    ViewPager2 viewPager = requireActivity().findViewById(R.id.viewPager);
                    if (viewPager != null) {
                        viewPager.setCurrentItem(0, true);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to save profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnSignUp.setEnabled(true);
                    btnSignUp.setBackgroundColor(getResources().getColor(R.color.darkGrey));
                });
    }
}