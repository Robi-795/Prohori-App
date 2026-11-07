package com.prohori.app.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.prohori.app.R;
import com.prohori.app.models.AppSnapshot;
import com.prohori.app.models.RiskResult;
import com.prohori.app.scanner.SecurityScanner;
import com.prohori.app.security.PermissionRules;
import com.prohori.app.security.RiskAnalyzer;
import com.prohori.app.utils.UiUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Detailed inspection of one app: info, rating with reasons, permissions, recommended action. */
public class AppDetailsActivity extends AppCompatActivity {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private String packageName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_details);

        packageName = getIntent().getStringExtra("package");
        if (packageName == null) {
            finish();
            return;
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        findViewById(R.id.btn_open_settings).setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.fromParts("package", packageName, null));
            try {
                startActivity(intent);
            } catch (Exception e) {
                UiUtils.toast(this, "Could not open Android app info for this app.");
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload each time so changes made in Android settings show up when you come back.
        load();
    }

    private void load() {
        final Context app = getApplicationContext();
        executor.execute(() -> {
            final AppSnapshot snapshot = SecurityScanner.snapshotFor(app, packageName);
            final RiskResult result = snapshot == null ? null : RiskAnalyzer.analyze(snapshot);
            Drawable icon = null;
            try {
                icon = app.getPackageManager().getApplicationIcon(packageName);
            } catch (Exception ignored) {
            }
            final Drawable finalIcon = icon;
            mainHandler.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (snapshot == null || result == null) {
                    UiUtils.toast(this, "This app is no longer installed or cannot be inspected.");
                    finish();
                    return;
                }
                render(snapshot, result, finalIcon);
            });
        });
    }

    private void render(AppSnapshot s, RiskResult r, Drawable icon) {
        if (icon != null) {
            ((ImageView) findViewById(R.id.iv_icon)).setImageDrawable(icon);
        }
        ((TextView) findViewById(R.id.tv_name)).setText(s.appName);
        ((TextView) findViewById(R.id.tv_package)).setText(s.packageName);
        UiUtils.styleBadge((TextView) findViewById(R.id.tv_badge), r.riskLevel, r.riskLevel.label);
        ((TextView) findViewById(R.id.tv_score)).setText("Risk score " + r.riskScore + " / 100");

        String info = "Version: " + s.versionName + " (" + s.versionCode + ")"
                + "\nInstall source: " + r.installSource
                + "\nInstalled: " + UiUtils.formatDate(s.firstInstallTime)
                + "\nLast updated: " + UiUtils.formatDate(s.lastUpdateTime)
                + "\nType: " + (s.systemApp ? "System app" : "User-installed app")
                + "\nTargets Android API: " + s.targetSdk
                + "\nAccessibility service on: " + yesNo(s.accessibilityEnabled)
                + "\nUsed as keyboard: " + yesNo(s.inputMethodEnabled)
                + "\nNotification access on: " + yesNo(s.notificationListenerEnabled)
                + "\nDevice admin active: " + yesNo(s.deviceAdminActive);
        ((TextView) findViewById(R.id.tv_info)).setText(info);

        List<String> bullets = new ArrayList<>();
        for (String reason : r.reasons) {
            bullets.add("\u2022 " + reason);
        }
        ((TextView) findViewById(R.id.tv_reasons)).setText(TextUtils.join("\n\n", bullets));
        ((TextView) findViewById(R.id.tv_reco)).setText(r.recommendation);

        renderPermissions(s);
    }

    private void renderPermissions(AppSnapshot s) {
        LinearLayout container = findViewById(R.id.perm_container);
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        List<String> sensitive = new ArrayList<>();
        List<String> other = new ArrayList<>();
        for (String permission : s.requestedPermissions) {
            if (PermissionRules.get(permission) != null) sensitive.add(permission);
            else other.add(permission);
        }
        // Heaviest permissions first.
        java.util.Collections.sort(sensitive, (a, b) ->
                PermissionRules.get(b).weight - PermissionRules.get(a).weight);

        TextView title = findViewById(R.id.tv_perm_title);
        title.setText("Sensitive permissions (" + sensitive.size() + ")");

        for (String permission : sensitive) {
            PermissionRules.Rule rule = PermissionRules.get(permission);
            View row = inflater.inflate(R.layout.item_permission, container, false);
            ((TextView) row.findViewById(R.id.tv_perm_name)).setText(rule.label);
            String state = rule.special ? "Requested" : (s.grantedPermissions.contains(permission)
                    ? "Granted" : "Requested, not granted");
            ((TextView) row.findViewById(R.id.tv_perm_state)).setText(
                    PermissionRules.shortName(permission) + "  |  " + state);
            ((TextView) row.findViewById(R.id.tv_perm_explain)).setText(rule.explanation);
            container.addView(row);
        }

        TextView otherView = findViewById(R.id.tv_other_perms);
        if (sensitive.isEmpty()) {
            otherView.setText("This app does not request any permission that Prohori tracks as sensitive.");
        } else if (other.isEmpty()) {
            otherView.setText("");
        } else {
            List<String> names = new ArrayList<>();
            for (String p : other) names.add(PermissionRules.shortName(p));
            otherView.setText("Other permissions (" + other.size() + "): " + TextUtils.join(", ", names));
        }
    }

    private static String yesNo(boolean value) {
        return value ? "Yes" : "No";
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
