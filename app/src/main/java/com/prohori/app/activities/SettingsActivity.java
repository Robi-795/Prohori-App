package com.prohori.app.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.messaging.FirebaseMessaging;
import com.prohori.app.R;
import com.prohori.app.firebase.FirebaseManager;
import com.prohori.app.utils.LocalStore;
import com.prohori.app.utils.UiUtils;

public class SettingsActivity extends AppCompatActivity {

    private LocalStore store;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        store = new LocalStore(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        SwitchMaterial swNotifications = findViewById(R.id.sw_notifications);
        SwitchMaterial swSystem = findViewById(R.id.sw_system_apps);
        SwitchMaterial swCloud = findViewById(R.id.sw_cloud);

        // Set the current values first, then attach listeners.
        swNotifications.setChecked(store.isNotificationsEnabled());
        swSystem.setChecked(store.isIncludeSystemApps());
        swCloud.setChecked(store.isCloudSyncEnabled());

        swNotifications.setOnCheckedChangeListener((button, checked) -> store.setNotificationsEnabled(checked));
        swSystem.setOnCheckedChangeListener((button, checked) -> store.setIncludeSystemApps(checked));
        swCloud.setOnCheckedChangeListener((button, checked) -> {
            store.setCloudSync(checked);
            if (checked) {
                FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                    String token = task.isSuccessful() ? task.getResult() : null;
                    FirebaseManager.get().saveDeviceInfo(store.deviceId(), token);
                });
            } else {
                UiUtils.toast(this, "Cloud sync is off. Data already in the cloud stays until you delete it.");
            }
        });

        findViewById(R.id.btn_privacy).setOnClickListener(v -> showText("Privacy information", R.string.privacy_text));
        findViewById(R.id.btn_terms).setOnClickListener(v -> showText("Terms and conditions", R.string.terms_text));
        findViewById(R.id.btn_delete_cloud).setOnClickListener(v -> confirmDeleteCloud());
    }

    private void showText(String title, int textRes) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(textRes)
                .setPositiveButton("Close", null)
                .show();
    }

    private void confirmDeleteCloud() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete cloud scan data?")
                .setMessage("This permanently deletes your scan history, alerts and device info from the cloud. "
                        + "Your account is not deleted. This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) ->
                        FirebaseManager.get().deleteAllUserData(new FirebaseManager.Callback<Integer>() {
                            @Override
                            public void onSuccess(Integer count) {
                                UiUtils.toast(SettingsActivity.this, "Deleted " + count + " cloud records.");
                            }

                            @Override
                            public void onError(String message) {
                                UiUtils.toast(SettingsActivity.this, "Could not delete: " + message);
                            }
                        }))
                .setNegativeButton("Cancel", null)
                .show();
    }
}
