package com.livisync.app;

public class AdminReportItem {
    private String reportId;
    private String reporterUid, reporterName, reporterEmail;
    private String reportedUid, reportedName, reportedEmail;
    private String reason;
    private long timestamp;
    private String status;

    public AdminReportItem(String reportId, String reporterUid, String reporterName, String reporterEmail,
                           String reportedUid, String reportedName, String reportedEmail,
                           String reason, long timestamp, String status) {
        this.reportId = reportId;
        this.reporterUid = reporterUid;
        this.reporterName = reporterName;
        this.reporterEmail = reporterEmail;
        this.reportedUid = reportedUid;
        this.reportedName = reportedName;
        this.reportedEmail = reportedEmail;
        this.reason = reason;
        this.timestamp = timestamp;
        this.status = status;
    }

    public String getReportId() { return reportId; }
    public String getReporterUid() { return reporterUid; }
    public String getReporterName() { return reporterName; }
    public String getReporterEmail() { return reporterEmail; }
    public String getReportedUid() { return reportedUid; }
    public String getReportedName() { return reportedName; }
    public String getReportedEmail() { return reportedEmail; }
    public String getReason() { return reason; }
    public long getTimestamp() { return timestamp; }
    public String getStatus() { return status; }
}