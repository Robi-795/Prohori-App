package com.prohori.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.prohori.app.R;
import com.prohori.app.adapters.AppsAdapter;
import com.prohori.app.models.ScanReport;
import com.prohori.app.utils.UiUtils;

/** Shows the summary and the apps with risk indicators for one scan (new or from history). */
public class ScanResultActivity extends AppCompatActivity {

    @Override
    @SuppressWarnings("deprecation")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_result);

        ScanReport report = (ScanReport) getIntent().getSerializableExtra("report");
        if (report == null) {
            finish();
            return;
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        ((TextView) findViewById(R.id.tv_scan_title)).setText(report.scanType);
        ((TextView) findViewById(R.id.tv_scan_sub)).setText(UiUtils.formatDate(report.scanDate) + "  |  "
                + report.totalApps + " apps inspected, " + report.riskyApps() + " to review");
        ((TextView) findViewById(R.id.tv_critical)).setText(String.valueOf(report.critical));
        ((TextView) findViewById(R.id.tv_high)).setText(String.valueOf(report.high));
        ((TextView) findViewById(R.id.tv_medium)).setText(String.valueOf(report.medium));
        ((TextView) findViewById(R.id.tv_low)).setText(String.valueOf(report.low));
        ((TextView) findViewById(R.id.tv_safe)).setText(String.valueOf(report.safe));

        TextView note = findViewById(R.id.tv_note);
        if (report.partial) {
            note.setText("History keeps only apps rated Medium or higher. Tap an app to analyse it again with its current state.");
        } else {
            note.setText("Tap an app to see its permissions, the reasons for its rating and what to do.");
        }

        RecyclerView list = findViewById(R.id.rv_findings);
        AppsAdapter adapter = new AppsAdapter(this, result -> {
            Intent intent = new Intent(this, AppDetailsActivity.class);
            intent.putExtra("package", result.packageName);
            startActivity(intent);
        });
        list.setAdapter(adapter);
        adapter.setItems(report.findings);

        findViewById(R.id.tv_empty).setVisibility(report.findings.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
