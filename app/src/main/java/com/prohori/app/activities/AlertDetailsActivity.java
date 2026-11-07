package com.prohori.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.prohori.app.R;
import com.prohori.app.firebase.FirebaseManager;
import com.prohori.app.models.SecurityAlert;
import com.prohori.app.utils.UiUtils;

public class AlertDetailsActivity extends AppCompatActivity {

    @Override
    @SuppressWarnings("deprecation")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert_details);

        final SecurityAlert alert = (SecurityAlert) getIntent().getSerializableExtra("alert");
        if (alert == null) {
            finish();
            return;
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        UiUtils.styleBadge((TextView) findViewById(R.id.tv_badge), alert.riskLevel,
                alert.riskLevel.label + "  " + alert.riskScore + "/100");
        ((TextView) findViewById(R.id.tv_app)).setText(alert.appName);
        ((TextView) findViewById(R.id.tv_package)).setText(alert.packageName);
        ((TextView) findViewById(R.id.tv_time)).setText("Detected " + UiUtils.formatDate(alert.timestamp));
        ((TextView) findViewById(R.id.tv_desc)).setText(alert.description);
        ((TextView) findViewById(R.id.tv_reco)).setText(alert.recommendation);

        if (!alert.isRead) {
            FirebaseManager.get().markAlertRead(alert.id);
        }

        findViewById(R.id.btn_view_app).setOnClickListener(v -> {
            Intent intent = new Intent(this, AppDetailsActivity.class);
            intent.putExtra("package", alert.packageName);
            startActivity(intent);
        });

        findViewById(R.id.btn_dismiss).setOnClickListener(v ->
                FirebaseManager.get().dismissAlert(alert.id, new FirebaseManager.Callback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        UiUtils.toast(AlertDetailsActivity.this, "Alert dismissed.");
                        finish();
                    }

                    @Override
                    public void onError(String message) {
                        UiUtils.toast(AlertDetailsActivity.this, "Could not dismiss: " + message);
                    }
                }));
    }
}
