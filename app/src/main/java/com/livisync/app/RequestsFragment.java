package com.livisync.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class RequestsFragment extends Fragment {

    RecyclerView rvIncoming, rvOutgoing;
    RequestAdapter incomingAdapter, outgoingAdapter;
    List<RequestItem> incomingList = new ArrayList<>();
    List<RequestItem> outgoingList = new ArrayList<>();

    FirebaseFirestore db;
    String myUid;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_requests, container, false);

        db = FirebaseFirestore.getInstance();
        myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        rvIncoming = view.findViewById(R.id.rvIncoming);
        rvOutgoing = view.findViewById(R.id.rvOutgoing);

        incomingAdapter = new RequestAdapter(incomingList, new RequestAdapter.OnRequestAction() {
            @Override
            public void onAccept(RequestItem item) {
                updateRequestStatus(item, "accepted");
            }
            @Override
            public void onDecline(RequestItem item) {
                updateRequestStatus(item, "declined");
            }
        });

        outgoingAdapter = new RequestAdapter(outgoingList, new RequestAdapter.OnRequestAction() {
            @Override public void onAccept(RequestItem item) {}
            @Override public void onDecline(RequestItem item) {}
        });

        rvIncoming.setLayoutManager(new LinearLayoutManager(getContext()));
        rvIncoming.setAdapter(incomingAdapter);

        rvOutgoing.setLayoutManager(new LinearLayoutManager(getContext()));
        rvOutgoing.setAdapter(outgoingAdapter);

        loadIncomingRequests();
        loadOutgoingRequests();

        return view;
    }

    private void loadIncomingRequests() {
        db.collection("matchRequests")
                .whereEqualTo("toUid", myUid)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(snap -> {
                    incomingList.clear();
                    List<DocumentSnapshot> docs = snap.getDocuments();
                    if (docs.isEmpty()) {
                        if (getActivity() != null)
                            getActivity().runOnUiThread(() -> incomingAdapter.updateList(incomingList));
                        return;
                    }

                    final int[] count = {0};
                    for (DocumentSnapshot doc : docs) {
                        String fromUid = doc.getString("fromUid");
                        String status = doc.getString("status");
                        String requestId = doc.getId();

                        db.collection("users").document(fromUid).get()
                                .addOnSuccessListener(userDoc -> {
                                    count[0]++;
                                    String name = userDoc.getString("name");
                                    incomingList.add(new RequestItem(requestId, fromUid, name, status, true));
                                    if (count[0] == docs.size()) {
                                        if (getActivity() != null)
                                            getActivity().runOnUiThread(() -> incomingAdapter.updateList(incomingList));
                                    }
                                });
                    }
                });
    }

    private void loadOutgoingRequests() {
        db.collection("matchRequests")
                .whereEqualTo("fromUid", myUid)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(snap -> {
                    outgoingList.clear();
                    List<DocumentSnapshot> docs = snap.getDocuments();
                    if (docs.isEmpty()) {
                        if (getActivity() != null)
                            getActivity().runOnUiThread(() -> outgoingAdapter.updateList(outgoingList));
                        return;
                    }

                    final int[] count = {0};
                    for (DocumentSnapshot doc : docs) {
                        String toUid = doc.getString("toUid");
                        String status = doc.getString("status");
                        String requestId = doc.getId();

                        db.collection("users").document(toUid).get()
                                .addOnSuccessListener(userDoc -> {
                                    count[0]++;
                                    String name = userDoc.getString("name");
                                    outgoingList.add(new RequestItem(requestId, toUid, name, status, false));
                                    if (count[0] == docs.size()) {
                                        if (getActivity() != null)
                                            getActivity().runOnUiThread(() -> outgoingAdapter.updateList(outgoingList));
                                    }
                                });
                    }
                });
    }

    private void updateRequestStatus(RequestItem item, String newStatus) {
        db.collection("matchRequests").document(item.getRequestId())
                .update("status", newStatus)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(getContext(),
                            newStatus.equals("accepted") ? "Request accepted!" : "Request declined",
                            Toast.LENGTH_SHORT).show();

                    if (newStatus.equals("accepted")) {
                        createMatch(item);
                    }

                    loadIncomingRequests();
                });
    }

    private void createMatch(RequestItem item) {
        java.util.Map<String, Object> match = new java.util.HashMap<>();
        match.put("user1", myUid);
        match.put("user2", item.getOtherUid());
        match.put("timestamp", System.currentTimeMillis());
        match.put("unread_" + myUid, false);
        match.put("unread_" + item.getOtherUid(), false);
        match.put("lastMessage", "New match! Say hello.");

        db.collection("matches").add(match);
    }
}