package com.prohori.app.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseUser;
import com.prohori.app.R;
import com.prohori.app.activities.AlertDetailsActivity;
import com.prohori.app.activities.MainActivity;
import com.prohori.app.adapters.AlertsAdapter;
import com.prohori.app.firebase.FirebaseManager;
import com.prohori.app.models.RiskLevel;
import com.prohori.app.models.ScanReport;
import com.prohori.app.models.SecurityAlert;
import com.prohori.app.scanner.SecurityScanner;
import com.prohori.app.utils.LocalStore;
import com.prohori.app.utils.UiUtils;

import java.util.List;

/** Dashboard: overview of the last scan, risk counts, shortcuts and recent alerts. */
public class HomeFragment extends Fragment {

    private TextView tvWelcome;
    private TextView tvStatusTitle;
    private TextView tvStatusSub;
    private TextView tvLastScan;
    private TextView tvRingPercent;
    private TextView tvTotal;
    private TextView tvRisky;
    private TextView tvCritical;
    private TextView tvHigh;
    private TextView tvMedium;
    private TextView tvLow;
    private TextView tvNoAlerts;
    private CircularProgressIndicator ring;
    private LinearLayout recentContainer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvWelcome = view.findViewById(R.id.tv_welcome);
        tvStatusTitle = view.findViewById(R.id.tv_status_title);
        tvStatusSub = view.findViewById(R.id.tv_status_sub);
        tvLastScan = view.findViewById(R.id.tv_last_scan);
        tvRingPercent = view.findViewById(R.id.tv_ring_percent);
        tvTotal = view.findViewById(R.id.tv_total);
        tvRisky = view.findViewById(R.id.tv_risky);
        tvCritical = view.findViewById(R.id.tv_critical);
        tvHigh = view.findViewById(R.id.tv_high);
        tvMedium = view.findViewById(R.id.tv_medium);
        tvLow = view.findViewById(R.id.tv_low);
        tvNoAlerts = view.findViewById(R.id.tv_no_alerts);
        ring = view.findViewById(R.id.ring);
        recentContainer = view.findViewById(R.id.recent_container);

        FirebaseUser user = FirebaseManager.get().currentUser();
        String name = "there";
        if (user != null && user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
            name = user.getDisplayName();
        }
        tvWelcome.setText("Welcome, " + name);

        view.findViewById(R.id.btn_quick).setOnClickListener(v ->
                ((MainActivity) requireActivity()).startScan(SecurityScanner.QUICK_SCAN));
        view.findViewById(R.id.btn_deep).setOnClickListener(v ->
                ((MainActivity) requireActivity()).startScan(SecurityScanner.DEEP_INSPECTION));
        view.findViewById(R.id.btn_history).setOnClickListener(v ->
                ((MainActivity) requireActivity()).selectTab(R.id.nav_history));
        view.findViewById(R.id.btn_alerts).setOnClickListener(v ->
                ((MainActivity) requireActivity()).selectTab(R.id.nav_alerts));
    }

    @Override
    public void onResume() {
        super.onResume();
        bindLastScan();
        loadRecentAlerts();
    }

    private void bindLastScan() {
        ScanReport r = new LocalStore(requireContext()).loadLastScan();
        if (r == null) {
            tvStatusTitle.setText("No scan yet");
            tvStatusTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            tvStatusSub.setText("Run a Quick Scan to inspect the apps on this device.");
            tvLastScan.setText("Last scan: never");
            tvRingPercent.setText("--");
            ring.setProgress(0);
            tvTotal.setText("0");
            tvRisky.setText("0");
            tvCritical.setText("0");
            tvHigh.setText("0");
            tvMedium.setText("0");
            tvLow.setText("0");
            return;
        }

        RiskLevel level;
        String title;
        if (r.critical > 0) {
            level = RiskLevel.CRITICAL;
            title = "Critical indicators found";
        } else if (r.high > 0) {
            level = RiskLevel.HIGH;
            title = "High-risk indicators found";
        } else if (r.medium > 0) {
            level = RiskLevel.MEDIUM;
            title = "Some apps need a review";
        } else {
            level = RiskLevel.SAFE;
            title = "No major indicators found";
        }
        int color = UiUtils.riskColor(requireContext(), level);
        tvStatusTitle.setText(title);
        tvStatusTitle.setTextColor(color);
        tvStatusSub.setText(r.riskyApps() + " of " + r.totalApps + " apps rated Medium or higher.");
        tvLastScan.setText("Last scan: " + UiUtils.formatDate(r.scanDate) + " (" + r.scanType + ")");

        int percent = r.totalApps == 0 ? 100 : Math.round(100f * (r.totalApps - r.riskyApps()) / r.totalApps);
        ring.setIndicatorColor(color);
        ring.setProgress(percent);
        tvRingPercent.setText(percent + "%");

        tvTotal.setText(String.valueOf(r.totalApps));
        tvRisky.setText(String.valueOf(r.riskyApps()));
        tvCritical.setText(String.valueOf(r.critical));
        tvHigh.setText(String.valueOf(r.high));
        tvMedium.setText(String.valueOf(r.medium));
        tvLow.setText(String.valueOf(r.low));
    }

    private void loadRecentAlerts() {
        recentContainer.removeAllViews();
        if (!new LocalStore(requireContext()).isCloudSyncEnabled()) {
            tvNoAlerts.setText("Cloud sync is off, so alerts are not kept. Turn it on in Profile > Settings to keep an alert history.");
            tvNoAlerts.setVisibility(View.VISIBLE);
            return;
        }
        FirebaseManager.get().loadAlerts(new FirebaseManager.Callback<List<SecurityAlert>>() {
            @Override
            public void onSuccess(List<SecurityAlert> alerts) {
                if (!isAdded()) return;
                recentContainer.removeAllViews();
                int shown = 0;
                for (final SecurityAlert alert : alerts) {
                    if (shown == 3) break;
                    View row = getLayoutInflater().inflate(R.layout.item_alert, recentContainer, false);
                    AlertsAdapter.bind(row, alert);
                    row.setOnClickListener(v -> {
                        Intent intent = new Intent(requireContext(), AlertDetailsActivity.class);
                        intent.putExtra("alert", alert);
                        startActivity(intent);
                    });
                    recentContainer.addView(row);
                    shown++;
                }
                if (shown == 0) {
                    tvNoAlerts.setText("No alerts yet. High and Critical findings from your scans appear here.");
                    tvNoAlerts.setVisibility(View.VISIBLE);
                } else {
                    tvNoAlerts.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                tvNoAlerts.setText("Could not load alerts: " + message);
                tvNoAlerts.setVisibility(View.VISIBLE);
            }
        });
    }
}
