package com.prohori.app.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Summary of one scan plus the apps that showed risk indicators. */
public class ScanReport implements Serializable {
    private static final long serialVersionUID = 1L;

    public String id;
    public String scanType = "";
    public long scanDate;
    public int totalApps;
    public int critical;
    public int high;
    public int medium;
    public int low;
    public int safe;
    /** True when loaded from cloud history, where only Medium and above are stored. */
    public boolean partial;
    public ArrayList<RiskResult> findings = new ArrayList<>();

    /** Apps that need a closer look: Critical + High + Medium. */
    public int riskyApps() {
        return critical + high + medium;
    }

    public static ScanReport fromResults(String scanType, long scanDate, List<RiskResult> all) {
        ScanReport report = new ScanReport();
        report.scanType = scanType;
        report.scanDate = scanDate;
        report.totalApps = all.size();
        for (RiskResult r : all) {
            switch (r.riskLevel) {
                case CRITICAL: report.critical++; break;
                case HIGH: report.high++; break;
                case MEDIUM: report.medium++; break;
                case LOW: report.low++; break;
                default: report.safe++; break;
            }
            if (r.riskLevel != RiskLevel.SAFE) {
                report.findings.add(r);
            }
        }
        Collections.sort(report.findings, new Comparator<RiskResult>() {
            @Override
            public int compare(RiskResult a, RiskResult b) {
                return b.riskScore - a.riskScore;
            }
        });
        return report;
    }
}
