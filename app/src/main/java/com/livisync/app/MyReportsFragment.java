package com.livisync.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class MyReportsFragment extends Fragment {

    private TextView tvReportCount;
    private RecyclerView rvMyReports;
    private FirebaseFirestore db;
    private MyReportAdapter adapter;
    private List<AdminReportItem> reportList = new ArrayList<>();
    private String myUid;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_reports, container, false);

        tvReportCount = view.findViewById(R.id.tvReportCount);
        rvMyReports = view.findViewById(R.id.rvMyReports);
        db = FirebaseFirestore.getInstance();
        myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        rvMyReports.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MyReportAdapter(reportList);
        rvMyReports.setAdapter(adapter);

        loadMyReports();

        return view;
    }

    private void loadMyReports() {
        db.collection("reports")
                .whereEqualTo("reporterUid", myUid)
                .addSnapshotListener((value, error) -> {

                    if (value != null) {
                        reportList.clear();
                        List<DocumentSnapshot> reportDocs = value.getDocuments();
                        if (reportDocs.isEmpty()) {
                            tvReportCount.setText("Total Reports: 0");
                            adapter.notifyDataSetChanged();
                            return;
                        }

                        final int[] processed = {0};
                        for (DocumentSnapshot reportDoc : reportDocs) {
                            String reportId = reportDoc.getId();
                            String reporterUid = reportDoc.getString("reporterUid");
                            String reportedUid = reportDoc.getString("reportedUid");
                            String reason = reportDoc.getString("reason");
                            Long timestamp = reportDoc.getLong("timestamp");
                            String status = reportDoc.getString("status");

                            if (timestamp == null) timestamp = 0L;
                            final long finalTimestamp = timestamp;

                            // For "My Reports", we mainly need the reported user's info
                            db.collection("users").document(reportedUid).get().addOnSuccessListener(reportedUserDoc -> {
                                String reportedName = reportedUserDoc.getString("name");
                                String reportedEmail = reportedUserDoc.getString("email");

                                reportList.add(new AdminReportItem(
                                        reportId,
                                        reporterUid, "Me", "", // reporter info (not needed for self view)
                                        reportedUid, reportedName != null ? reportedName : "Unknown", reportedEmail != null ? reportedEmail : "",
                                        reason, finalTimestamp, status
                                ));

                                processed[0]++;
                                if (processed[0] == reportDocs.size()) {
                                    tvReportCount.setText("Total Reports: " + reportList.size());
                                    adapter.notifyDataSetChanged();
                                }
                            });
                        }
                    }
                });
    }
}
