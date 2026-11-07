package com.prohori.app.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.prohori.app.R;
import com.prohori.app.activities.ScanResultActivity;
import com.prohori.app.adapters.HistoryAdapter;
import com.prohori.app.firebase.FirebaseManager;
import com.prohori.app.models.ScanReport;
import com.prohori.app.utils.LocalStore;
import com.prohori.app.utils.UiUtils;

import java.util.ArrayList;
import java.util.List;

/** Past scans saved in the user's private cloud history. */
public class HistoryFragment extends Fragment {

    private HistoryAdapter adapter;
    private ProgressBar progress;
    private TextView tvEmpty;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        progress = view.findViewById(R.id.progress);
        tvEmpty = view.findViewById(R.id.tv_empty);
        RecyclerView list = view.findViewById(R.id.rv_history);

        adapter = new HistoryAdapter(
                report -> {
                    Intent intent = new Intent(requireContext(), ScanResultActivity.class);
                    intent.putExtra("report", report);
                    startActivity(intent);
                },
                this::confirmDeleteOne);
        list.setAdapter(adapter);

        view.findViewById(R.id.btn_delete_all).setOnClickListener(v -> confirmDeleteAll());
    }

    @Override
    public void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        if (!new LocalStore(requireContext()).isCloudSyncEnabled()) {
            adapter.setItems(new ArrayList<ScanReport>());
            tvEmpty.setText("Cloud sync is off, so scans are not kept. Turn it on in Profile > Settings.");
            tvEmpty.setVisibility(View.VISIBLE);
            return;
        }
        progress.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        FirebaseManager.get().loadScans(new FirebaseManager.Callback<List<ScanReport>>() {
            @Override
            public void onSuccess(List<ScanReport> reports) {
                if (!isAdded()) return;
                progress.setVisibility(View.GONE);
                adapter.setItems(reports);
                if (reports.isEmpty()) {
                    tvEmpty.setText("No scans saved yet. Run a scan and it will appear here.");
                    tvEmpty.setVisibility(View.VISIBLE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                progress.setVisibility(View.GONE);
                tvEmpty.setText("Could not load history: " + message);
                tvEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    private void confirmDeleteOne(final ScanReport report) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete this scan?")
                .setMessage("The " + report.scanType + " from " + UiUtils.formatDate(report.scanDate)
                        + " will be removed from your history.")
                .setPositiveButton("Delete", (dialog, which) ->
                        FirebaseManager.get().deleteScan(report.id, new FirebaseManager.Callback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                if (isAdded()) load();
                            }

                            @Override
                            public void onError(String message) {
                                if (isAdded()) UiUtils.toast(requireContext(), "Could not delete: " + message);
                            }
                        }))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDeleteAll() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete all scan history?")
                .setMessage("Every saved scan will be removed from your cloud history. This cannot be undone.")
                .setPositiveButton("Delete all", (dialog, which) ->
                        FirebaseManager.get().deleteAllMine(FirebaseManager.SCANS, new FirebaseManager.Callback<Integer>() {
                            @Override
                            public void onSuccess(Integer count) {
                                if (!isAdded()) return;
                                UiUtils.toast(requireContext(), "Deleted " + count + " scans.");
                                load();
                            }

                            @Override
                            public void onError(String message) {
                                if (isAdded()) UiUtils.toast(requireContext(), "Could not delete: " + message);
                            }
                        }))
                .setNegativeButton("Cancel", null)
                .show();
    }
}
