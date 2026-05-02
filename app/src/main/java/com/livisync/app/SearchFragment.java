package com.livisync.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment {

    private EditText etSearch;
    private View rootLayout;
    private RecyclerView rvSearchResults;
    private RoommateAdapter adapter;
    private List<RoommateItem> allRoommates = new ArrayList<>();
    private List<RoommateItem> filteredList = new ArrayList<>();

    private FirebaseFirestore db;
    private String myUid;
    private Preferences myPrefs;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        db = FirebaseFirestore.getInstance();
        myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        etSearch = view.findViewById(R.id.etSearch);
        rootLayout = view.findViewById(R.id.searchRootLayout);
        rvSearchResults = view.findViewById(R.id.rvSearchResults);

        rvSearchResults.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new RoommateAdapter(filteredList, new RoommateAdapter.OnActionListener() {
            @Override
            public void onSendRequest(RoommateItem item) {
                sendMatchRequest(item);
            }

            @Override
            public void onViewProfile(RoommateItem item) {
                Intent intent = new Intent(getActivity(), ViewProfileActivity.class);
                intent.putExtra("uid", item.getUid());
                intent.putExtra("score", item.getScore());
                startActivity(intent);
            }
        });
        rvSearchResults.setAdapter(adapter);

        rootLayout.setOnClickListener(v -> {
            if (etSearch.hasFocus()) {
                etSearch.clearFocus();
                hideKeyboard(v);
            }
        });

        setupSearchListener();
        loadMyPreferencesThenUsers();

        return view;
    }

    private void setupSearchListener() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filter(String text) {
        filteredList.clear();
        if (!text.isEmpty()) {
            for (RoommateItem item : allRoommates) {
                if (item.getName().toLowerCase().contains(text.toLowerCase())) {
                    filteredList.add(item);
                }
            }
        }
        adapter.updateList(filteredList);
    }

    private void loadMyPreferencesThenUsers() {
        FirestoreHelper.getPreferences(myUid, doc -> {
            if (doc.exists()) {
                myPrefs = new Preferences();
                myPrefs.setSleepSchedule(doc.getString("sleepSchedule"));
                myPrefs.setCity(doc.getString("city"));
                Long clean = doc.getLong("cleanliness");
                Long min = doc.getLong("budgetMin");
                Long max = doc.getLong("budgetMax");
                myPrefs.setCleanliness(clean != null ? clean.intValue() : 3);
                myPrefs.setBudgetMin(min != null ? min.intValue() : 0);
                myPrefs.setBudgetMax(max != null ? max.intValue() : 99999);
                myPrefs.setSmokingAllowed(Boolean.TRUE.equals(doc.getBoolean("smokingAllowed")));
                myPrefs.setPetsAllowed(Boolean.TRUE.equals(doc.getBoolean("petsAllowed")));
                myPrefs.setGuestsAllowed(Boolean.TRUE.equals(doc.getBoolean("guestsAllowed")));

                loadAllUsers();
            }
        });
    }

    private void loadAllUsers() {
        FirestoreHelper.getAllUsers(userSnapshots -> {
            List<DocumentSnapshot> userDocs = userSnapshots.getDocuments();

            allRoommates.clear();
            final int[] processed = {0};

            for (DocumentSnapshot userDoc : userDocs) {
                String uid = userDoc.getString("uid");

                if (uid == null || uid.equals(myUid)) {
                    processed[0]++;
                    continue;
                }

                String name = userDoc.getString("name");
                String bio = userDoc.getString("bio");

                FirestoreHelper.getPreferences(uid, prefDoc -> {
                    processed[0]++;

                    if (prefDoc.exists()) {
                        Preferences theirPrefs = new Preferences();
                        theirPrefs.setSleepSchedule(prefDoc.getString("sleepSchedule"));
                        theirPrefs.setCity(prefDoc.getString("city"));
                        Long clean = prefDoc.getLong("cleanliness");
                        Long min = prefDoc.getLong("budgetMin");
                        Long max = prefDoc.getLong("budgetMax");
                        theirPrefs.setCleanliness(clean != null ? clean.intValue() : 3);
                        theirPrefs.setBudgetMin(min != null ? min.intValue() : 0);
                        theirPrefs.setBudgetMax(max != null ? max.intValue() : 99999);
                        theirPrefs.setSmokingAllowed(Boolean.TRUE.equals(prefDoc.getBoolean("smokingAllowed")));
                        theirPrefs.setPetsAllowed(Boolean.TRUE.equals(prefDoc.getBoolean("petsAllowed")));
                        theirPrefs.setGuestsAllowed(Boolean.TRUE.equals(prefDoc.getBoolean("guestsAllowed")));

                        int score = CompatibilityScorer.calculate(myPrefs, theirPrefs);
                        String budgetRange = prefDoc.getLong("budgetMin") + "-" + prefDoc.getLong("budgetMax");
                        String sleep = theirPrefs.getSleepSchedule() != null ? theirPrefs.getSleepSchedule() : "-";
                        String city = theirPrefs.getCity() != null ? theirPrefs.getCity() : "-";

                        allRoommates.add(new RoommateItem(uid, name, bio, city, sleep, budgetRange, score));
                    }
                });
            }
        });
    }

    private void sendMatchRequest(RoommateItem item) {
        java.util.Map<String, Object> request = new java.util.HashMap<>();
        request.put("fromUid", myUid);
        request.put("toUid", item.getUid());
        request.put("status", "pending");
        request.put("timestamp", System.currentTimeMillis());

        db.collection("matchRequests").add(request)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(getContext(), "Request sent to " + item.getName(), Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
