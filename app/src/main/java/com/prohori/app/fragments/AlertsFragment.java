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

import com.prohori.app.R;
import com.prohori.app.activities.AlertDetailsActivity;
import com.prohori.app.adapters.AlertsAdapter;
import com.prohori.app.firebase.FirebaseManager;
import com.prohori.app.models.SecurityAlert;
import com.prohori.app.utils.LocalStore;

import java.util.List;

/** List of security alerts (Critical and High findings), newest first. */
public class AlertsFragment extends Fragment {

    private AlertsAdapter adapter;
    private ProgressBar progress;
    private TextView tvEmpty;
    private TextView tvUnread;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_alerts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        progress = view.findViewById(R.id.progress);
        tvEmpty = view.findViewById(R.id.tv_empty);
        tvUnread = view.findViewById(R.id.tv_unread);
        RecyclerView list = view.findViewById(R.id.rv_alerts);
        adapter = new AlertsAdapter(alert -> {
            Intent intent = new Intent(requireContext(), AlertDetailsActivity.class);
            intent.putExtra("alert", alert);
            startActivity(intent);
        });
        list.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        if (!new LocalStore(requireContext()).isCloudSyncEnabled()) {
            adapter.setItems(new java.util.ArrayList<SecurityAlert>());
            tvUnread.setText("");
            tvEmpty.setText("Cloud sync is off, so alerts are not kept. Turn it on in Profile > Settings.");
            tvEmpty.setVisibility(View.VISIBLE);
            return;
        }
        progress.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        FirebaseManager.get().loadAlerts(new FirebaseManager.Callback<List<SecurityAlert>>() {
            @Override
            public void onSuccess(List<SecurityAlert> alerts) {
                if (!isAdded()) return;
                progress.setVisibility(View.GONE);
                adapter.setItems(alerts);
                int unread = 0;
                for (SecurityAlert a : alerts) {
                    if (!a.isRead) unread++;
                }
                tvUnread.setText(alerts.isEmpty() ? "" : unread + " unread of " + alerts.size());
                if (alerts.isEmpty()) {
                    tvEmpty.setText("No alerts. High and Critical findings from your scans appear here.");
                    tvEmpty.setVisibility(View.VISIBLE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                progress.setVisibility(View.GONE);
                tvEmpty.setText("Could not load alerts: " + message);
                tvEmpty.setVisibility(View.VISIBLE);
            }
        });
    }
}
