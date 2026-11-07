package com.prohori.app.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.prohori.app.R;
import com.prohori.app.adapters.AppsAdapter;
import com.prohori.app.models.RiskResult;
import com.prohori.app.scanner.SecurityScanner;
import com.prohori.app.utils.LocalStore;
import com.prohori.app.utils.UiUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Lists every inspected app with its risk badge, with search. */
public class InstalledAppsActivity extends AppCompatActivity {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<RiskResult> all = new ArrayList<>();

    private AppsAdapter adapter;
    private ProgressBar progress;
    private TextView tvCount;
    private TextInputEditText etSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_installed_apps);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        progress = findViewById(R.id.progress);
        tvCount = findViewById(R.id.tv_count);
        etSearch = findViewById(R.id.et_search);

        RecyclerView list = findViewById(R.id.rv_apps);
        adapter = new AppsAdapter(this, result -> {
            Intent intent = new Intent(this, AppDetailsActivity.class);
            intent.putExtra("package", result.packageName);
            startActivity(intent);
        });
        list.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                applyFilter();
            }
        });

        loadApps();
    }

    private void loadApps() {
        progress.setVisibility(View.VISIBLE);
        final Context app = getApplicationContext();
        final boolean includeSystem = new LocalStore(app).isIncludeSystemApps();
        executor.execute(() -> {
            List<RiskResult> results;
            try {
                results = SecurityScanner.analyzeAll(app, includeSystem, false);
            } catch (Exception e) {
                results = new ArrayList<>();
            }
            final List<RiskResult> finalResults = results;
            mainHandler.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                all.clear();
                all.addAll(finalResults);
                progress.setVisibility(View.GONE);
                applyFilter();
            });
        });
    }

    private void applyFilter() {
        String query = UiUtils.text(etSearch).trim().toLowerCase();
        List<RiskResult> shown = new ArrayList<>();
        for (RiskResult r : all) {
            if (query.isEmpty()
                    || r.appName.toLowerCase().contains(query)
                    || r.packageName.toLowerCase().contains(query)) {
                shown.add(r);
            }
        }
        adapter.setItems(shown);
        tvCount.setText(shown.size() + " apps");
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
