package com.livisync.app;

import android.util.Log;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class FirestoreHelper {

    private static final String TAG = "FirestoreHelper";
    private static FirebaseFirestore db = FirebaseFirestore.getInstance();

    public static void getUserProfile(String uid, OnSuccessListener<DocumentSnapshot> listener) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(listener)
                .addOnFailureListener(e -> Log.e(TAG, "Error getting user profile", e));
    }

    public static void getPreferences(String uid, OnSuccessListener<DocumentSnapshot> listener) {
        db.collection("preferences").document(uid).get()
                .addOnSuccessListener(listener)
                .addOnFailureListener(e -> Log.e(TAG, "Error getting preferences", e));
    }

    public static void getAllUsers(OnSuccessListener<QuerySnapshot> listener) {
        db.collection("users").get()
                .addOnSuccessListener(listener)
                .addOnFailureListener(e -> Log.e(TAG, "Error getting all users", e));
    }

    public static void getAllPreferences(OnSuccessListener<QuerySnapshot> listener) {
        db.collection("preferences").get()
                .addOnSuccessListener(listener)
                .addOnFailureListener(e -> Log.e(TAG, "Error getting all preferences", e));
    }
}