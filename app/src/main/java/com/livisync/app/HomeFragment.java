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
import com.google.firebase.firestore.ListenerRegistration;

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

    private ListenerRegistration requestsListener, matchesListener1, matchesListener2;

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
        startStatsListeners();

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
        List<String> excludedUids = new ArrayList<>();
        excludedUids.add(myUid);

        db.collection("matchRequests")
                .whereEqualTo("fromUid", myUid)
                .get()
                .addOnSuccessListener(snap1 -> {
                    for (DocumentSnapshot doc : snap1.getDocuments()) {
                        excludedUids.add(doc.getString("toUid"));
                    }
                    db.collection("matchRequests")
                            .whereEqualTo("toUid", myUid)
                            .get()
                            .addOnSuccessListener(snap2 -> {
                                for (DocumentSnapshot doc : snap2.getDocuments()) {
                                    excludedUids.add(doc.getString("fromUid"));
                                }
                                
                                db.collection("matches")
                                        .whereEqualTo("user1", myUid)
                                        .get()
                                        .addOnSuccessListener(snap3 -> {
                                            for (DocumentSnapshot doc : snap3.getDocuments()) {
                                                excludedUids.add(doc.getString("user2"));
                                            }
                                            db.collection("matches")
                                                    .whereEqualTo("user2", myUid)
                                                    .get()
                                                    .addOnSuccessListener(snap4 -> {
                                                        for (DocumentSnapshot doc : snap4.getDocuments()) {
                                                            excludedUids.add(doc.getString("user1"));
                                                        }
                                                        fetchAndFilterUsers(excludedUids);
                                                    });
                                        });
                            });
                });
    }

    private void fetchAndFilterUsers(List<String> excludedUids) {
        FirestoreHelper.getAllUsers(userSnapshots -> {
            List<DocumentSnapshot> userDocs = userSnapshots.getDocuments();

            allRoommates.clear();
            if (userDocs.isEmpty()) {
                sortAndDisplay();
                return;
            }

            final int[] processed = {0};
            for (DocumentSnapshot userDoc : userDocs) {
                String uid = userDoc.getString("uid");

                if (uid == null || excludedUids.contains(uid)) {
                    processed[0]++;
                    if (processed[0] == userDocs.size()) {
                        sortAndDisplay();
                    }
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
            } else {
                tvTopMatch.setText("0%");
            }
        });
    }

    private void startStatsListeners() {
        // Real-time pending requests
        requestsListener = db.collection("matchRequests")
                .whereEqualTo("toUid", myUid)
                .whereEqualTo("status", "pending")
                .addSnapshotListener((snap, e) -> {
                    if (snap != null && getActivity() != null) {
                        getActivity().runOnUiThread(() ->
                                tvPendingRequests.setText(String.valueOf(snap.size())));
                    }
                });

        // Real-time unread chats
        updateUnreadCount();
    }

    private int unreadCount1 = 0;
    private int unreadCount2 = 0;

    private void updateUnreadCount() {
        matchesListener1 = db.collection("matches")
                .whereEqualTo("user1", myUid)
                .whereEqualTo("unread_" + myUid, true)
                .addSnapshotListener((snap, e) -> {
                    if (snap != null) {
                        unreadCount1 = snap.size();
                        displayTotalUnread();
                    }
                });

        matchesListener2 = db.collection("matches")
                .whereEqualTo("user2", myUid)
                .whereEqualTo("unread_" + myUid, true)
                .addSnapshotListener((snap, e) -> {
                    if (snap != null) {
                        unreadCount2 = snap.size();
                        displayTotalUnread();
                    }
                });
    }

    private void displayTotalUnread() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() ->
                    tvUnreadChats.setText(String.valueOf(unreadCount1 + unreadCount2)));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (requestsListener != null) requestsListener.remove();
        if (matchesListener1 != null) matchesListener1.remove();
        if (matchesListener2 != null) matchesListener2.remove();
    }

    private void sendMatchRequest(RoommateItem item) {
        String targetUid = item.getUid();

        db.collection("matchRequests")
                .whereEqualTo("fromUid", myUid)
                .whereEqualTo("toUid", targetUid)
                .get()
                .addOnSuccessListener(snap1 -> {
                    if (!snap1.isEmpty()) {
                        handleInteracted(item, "Request already sent");
                        return;
                    }

                    db.collection("matchRequests")
                            .whereEqualTo("fromUid", targetUid)
                            .whereEqualTo("toUid", myUid)
                            .get()
                            .addOnSuccessListener(snap2 -> {
                                if (!snap2.isEmpty()) {
                                    handleInteracted(item, "They already sent you a request!");
                                    return;
                                }

                                db.collection("matches")
                                        .whereEqualTo("user1", myUid)
                                        .whereEqualTo("user2", targetUid)
                                        .get()
                                        .addOnSuccessListener(snap3 -> {
                                            if (!snap3.isEmpty()) {
                                                handleInteracted(item, "Already friends!");
                                                return;
                                            }

                                            db.collection("matches")
                                                    .whereEqualTo("user1", targetUid)
                                                    .whereEqualTo("user2", myUid)
                                                    .get()
                                                    .addOnSuccessListener(snap4 -> {
                                                        if (!snap4.isEmpty()) {
                                                            handleInteracted(item, "Already friends!");
                                                            return;
                                                        }

                                                        executeSendRequest(item);
                                                    });
                                        });
                            });
                });
    }

    private void handleInteracted(RoommateItem item, String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        allRoommates.remove(item);
        adapter.updateList(allRoommates);
    }

    private void executeSendRequest(RoommateItem item) {
        java.util.Map<String, Object> request = new java.util.HashMap<>();
        request.put("fromUid", myUid);
        request.put("toUid", item.getUid());
        request.put("status", "pending");
        request.put("timestamp", System.currentTimeMillis());

        db.collection("matchRequests").add(request)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(getContext(), "Request sent to " + item.getName(), Toast.LENGTH_SHORT).show();
                    allRoommates.remove(item);
                    adapter.updateList(allRoommates);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}