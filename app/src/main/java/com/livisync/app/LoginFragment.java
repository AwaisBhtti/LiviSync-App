package com.livisync.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import kotlinx.coroutines.flow.SharedFlow;

public class LoginFragment extends Fragment {

    private FirebaseAuth mAuth;
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
        mAuth=FirebaseAuth.getInstance();

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
            if (inputPassword.length() < 8) {
                etPassword.setError("Password must be at least 8 characters");
                return;
            }

            btnLogin.setBackgroundColor(getResources().getColor(R.color.grey));
            mAuth.signInWithEmailAndPassword(inputEmail, inputPassword).addOnSuccessListener(authResult -> {
                startActivity(new Intent(getActivity(), MainActivity.class));
                requireActivity().finish();
            })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Login failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        btnLogin.setBackgroundColor(getResources().getColor(R.color.darkGrey));
                    });
        });
    }
}