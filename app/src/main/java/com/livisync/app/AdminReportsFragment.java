package com.livisync.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AdminReportsFragment extends Fragment {

    private TextView tvReportCount;
    private RecyclerView rvAdminReports;
    private FirebaseFirestore db;
    private AdminReportAdapter adapter;
    private List<AdminReportItem> reportList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_reports, container, false);

        tvReportCount = view.findViewById(R.id.tvReportCount);
        rvAdminReports = view.findViewById(R.id.rvAdminReports);
        db = FirebaseFirestore.getInstance();

        rvAdminReports.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AdminReportAdapter(reportList, new AdminReportAdapter.OnReportActionListener() {
            @Override
            public void onDismiss(AdminReportItem report) {
                showActionConfirmation("Dismiss Report", "Are you sure you want to dismiss this report?", () -> 
                    updateReportStatus(report.getReportId(), "dismissed", "Report dismissed"));
            }

            @Override
            public void onSuspend(AdminReportItem report) {
                showActionConfirmation("Suspend User", "Are you sure you want to suspend " + report.getReportedName() + "? They will no longer be able to log in.", () -> 
                    suspendUserAndResolveReport(report));
            }
        });
        rvAdminReports.setAdapter(adapter);

        loadReports();

        return view;
    }

    private void loadReports() {
        db.collection("reports")
                .whereEqualTo("status", "pending")
                .addSnapshotListener((value, error) -> {
            if (error != null) {
                if (getContext() != null)
                    Toast.makeText(getContext(), "Error loading reports", Toast.LENGTH_SHORT).show();
                return;
            }

            if (value != null) {
                reportList.clear();
                List<DocumentSnapshot> reportDocs = value.getDocuments();
                if (reportDocs.isEmpty()) {
                    tvReportCount.setText("Total Pending Reports: 0");
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

                    db.collection("users").document(reporterUid).get().addOnSuccessListener(reporterDocSnap -> {
                        String reporterName = reporterDocSnap.getString("name");
                        String reporterEmail = reporterDocSnap.getString("email");

                        db.collection("users").document(reportedUid).get().addOnSuccessListener(reportedUserDoc -> {
                            String reportedName = reportedUserDoc.getString("name");
                            String reportedEmail = reportedUserDoc.getString("email");

                            reportList.add(new AdminReportItem(
                                    reportId,
                                    reporterUid, reporterName != null ? reporterName : "Unknown", reporterEmail != null ? reporterEmail : "",
                                    reportedUid, reportedName != null ? reportedName : "Unknown", reportedEmail != null ? reportedEmail : "",
                                    reason, finalTimestamp, status
                            ));

                            processed[0]++;
                            if (processed[0] == reportDocs.size()) {
                                tvReportCount.setText("Total Pending Reports: " + reportList.size());
                                adapter.notifyDataSetChanged();
                            }
                        });
                    });
                }
            }
        });
    }

    private void showActionConfirmation(String title, String message, Runnable action) {
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Yes", (dialog, which) -> action.run())
                .setNegativeButton("No", null)
                .show();
    }

    private void updateReportStatus(String reportId, String status, String successMessage) {
        db.collection("reports").document(reportId).update("status", status)
                .addOnSuccessListener(aVoid -> {
                    if (getContext() != null)
                        Toast.makeText(getContext(), successMessage, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null)
                        Toast.makeText(getContext(), "Failed to update report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void suspendUserAndResolveReport(AdminReportItem report) {
        db.collection("users").document(report.getReportedUid()).update("role", "suspended")
                .addOnSuccessListener(aVoid -> {
                    updateReportStatus(report.getReportId(), "resolved", "User suspended and report resolved");
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null)
                        Toast.makeText(getContext(), "Failed to suspend user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}