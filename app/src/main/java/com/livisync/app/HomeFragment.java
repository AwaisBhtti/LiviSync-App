package com.livisync.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomeFragment extends Fragment {

    RecyclerView rvRoommates;
    RoommateAdapter adapter;
    List<RoommateItem> allRoommates = new ArrayList<>();
    TextView tvTopMatch, tvPendingRequests, tvUnreadChats;
    Spinner spFilterBudget, spFilterSleep, spFilterPets;

    FirebaseFirestore db;
    String myUid;
    Preferences myPrefs;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        db = FirebaseFirestore.getInstance();
        myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        rvRoommates = view.findViewById(R.id.rvRoommates);
        tvTopMatch = view.findViewById(R.id.tvTopMatch);
        tvPendingRequests = view.findViewById(R.id.tvPendingRequests);
        tvUnreadChats = view.findViewById(R.id.tvUnreadChats);
        spFilterBudget = view.findViewById(R.id.spFilterBudget);
        spFilterSleep = view.findViewById(R.id.spFilterSleep);
        spFilterPets = view.findViewById(R.id.spFilterPets);

        setupFilters();

        rvRoommates.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new RoommateAdapter(allRoommates, new RoommateAdapter.OnActionListener() {
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
        rvRoommates.setAdapter(adapter);

        loadMyPreferencesThenUsers();
        loadStats();

        return view;
    }

    private void setupFilters() {
        ArrayAdapter<String> budgetAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"Budget", "Low", "Mid", "High"});
        budgetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spFilterBudget.setAdapter(budgetAdapter);

        ArrayAdapter<String> sleepAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"Sleep", "Early Bird", "Late Night", "Night Owl", "Flexible"});
        sleepAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spFilterSleep.setAdapter(sleepAdapter);

        ArrayAdapter<String> petsAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"Pets", "Allowed", "Not Allowed"});
        petsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spFilterPets.setAdapter(petsAdapter);
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

                // Skip yourself
                if (uid == null || uid.equals(myUid)) {
                    processed[0]++;
                    if (processed[0] == userDocs.size()) {
                        sortAndDisplay();
                    }
                    continue;
                }

                String name = userDoc.getString("name");
                String bio = userDoc.getString("bio");

                // Get their preferences to calculate score
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

                    if (processed[0] == userDocs.size()) {
                        sortAndDisplay();
                    }
                });
            }
        });
    }

    private void sortAndDisplay() {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            Collections.sort(allRoommates, (a, b) -> b.getScore() - a.getScore());
            adapter.updateList(allRoommates);

            if (!allRoommates.isEmpty()) {
                tvTopMatch.setText(allRoommates.get(0).getScore() + "%");
            }
        });
    }

    private void loadStats() {
        db.collection("matchRequests")
                .whereEqualTo("toUid", myUid)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(snap -> {
                    if (getActivity() != null)
                        getActivity().runOnUiThread(() ->
                                tvPendingRequests.setText(String.valueOf(snap.size())));
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
}