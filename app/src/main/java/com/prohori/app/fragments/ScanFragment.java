package com.prohori.app.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.prohori.app.R;
import com.prohori.app.activities.InstalledAppsActivity;
import com.prohori.app.activities.ScanResultActivity;
import com.prohori.app.firebase.FirebaseManager;
import com.prohori.app.models.ScanReport;
import com.prohori.app.notifications.NotificationHelper;
import com.prohori.app.scanner.SecurityScanner;
import com.prohori.app.utils.LocalStore;
import com.prohori.app.utils.UiUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Starts Quick Scan and Deep Inspection and sends the result to the result screen. */
public class ScanFragment extends Fragment {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private MaterialButton btnQuick;
    private MaterialButton btnDeep;
    private MaterialButton btnInstalled;
    private View progressGroup;
    private TextView tvProgress;
    private boolean scanning;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_scan, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        btnQuick = view.findViewById(R.id.btn_quick);
        btnDeep = view.findViewById(R.id.btn_deep);
        btnInstalled = view.findViewById(R.id.btn_installed);
        progressGroup = view.findViewById(R.id.progress_group);
        tvProgress = view.findViewById(R.id.tv_progress);

        btnQuick.setOnClickListener(v -> runScan(SecurityScanner.QUICK_SCAN));
        btnDeep.setOnClickListener(v -> confirmDeep());
        btnInstalled.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), InstalledAppsActivity.class)));

        // The dashboard buttons open this tab and ask for a scan to start straight away.
        Bundle args = getArguments();
        if (args != null && args.getString("autostart") != null) {
            String type = args.getString("autostart");
            args.remove("autostart");
            if (SecurityScanner.DEEP_INSPECTION.equals(type)) {
                confirmDeep();
            } else {
                runScan(SecurityScanner.QUICK_SCAN);
            }
        }
    }

    private void confirmDeep() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Start Deep Inspection?")
                .setMessage("This checks every app, including system apps, and the special services each app "
                        + "declares. It can take longer than a Quick Scan.")
                .setPositiveButton("Start", (dialog, which) -> runScan(SecurityScanner.DEEP_INSPECTION))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void runScan(final String scanType) {
        if (scanning) return;
        scanning = true;
        setBusy(true, scanType);

        final Context app = requireContext().getApplicationContext();
        final LocalStore store = new LocalStore(app);
        final boolean includeSystem = store.isIncludeSystemApps();

        executor.execute(() -> {
            final ScanReport report;
            try {
                report = SecurityScanner.scan(app, scanType, includeSystem);
            } catch (Exception e) {
                mainHandler.post(() -> {
                    scanning = false;
                    if (isAdded()) {
                        setBusy(false, scanType);
                        UiUtils.toast(requireContext(), "The scan could not finish: " + e.getMessage());
                    }
                });
                return;
            }
            store.saveLastScan(report);
            mainHandler.post(() -> finishScan(app, store, report));
        });
    }

    private void finishScan(final Context app, final LocalStore store, ScanReport report) {
        scanning = false;
        final boolean visible = isAdded();
        if (visible) {
            setBusy(false, report.scanType);
            Intent intent = new Intent(requireContext(), ScanResultActivity.class);
            intent.putExtra("report", report);
            startActivity(intent);
        }

        if (store.isCloudSyncEnabled() && FirebaseManager.get().uid() != null) {
            FirebaseManager manager = FirebaseManager.get();
            manager.saveScan(report, new FirebaseManager.Callback<String>() {
                @Override
                public void onSuccess(String id) {
                    UiUtils.toast(app, "Scan saved to your private history.");
                }

                @Override
                public void onError(String message) {
                    UiUtils.toast(app, "Could not save scan history: " + message);
                }
            });
            manager.syncAlerts(report.findings, new FirebaseManager.Callback<Integer>() {
                @Override
                public void onSuccess(Integer newAlerts) {
                    // If you left the app during a long scan, a notification brings you back.
                    if (newAlerts > 0 && !visible && store.isNotificationsEnabled()) {
                        NotificationHelper.show(app, "Prohori: " + newAlerts + " new security alert(s)",
                                "Open Prohori to review the apps that were flagged.");
                    }
                }

                @Override
                public void onError(String message) {
                    // Alerts are a bonus; the scan result is already on screen.
                }
            });
        }
    }

    private void setBusy(boolean busy, String scanType) {
        progressGroup.setVisibility(busy ? View.VISIBLE : View.GONE);
        tvProgress.setText(busy ? "Running " + scanType + "..." : "");
        btnQuick.setEnabled(!busy);
        btnDeep.setEnabled(!busy);
        btnInstalled.setEnabled(!busy);
    }

    @Override
    public void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
