package com.prohori.app.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;
import com.prohori.app.R;
import com.prohori.app.firebase.FirebaseManager;
import com.prohori.app.fragments.AlertsFragment;
import com.prohori.app.fragments.HistoryFragment;
import com.prohori.app.fragments.HomeFragment;
import com.prohori.app.fragments.ProfileFragment;
import com.prohori.app.fragments.ScanFragment;
import com.prohori.app.utils.LocalStore;

/** Hosts the five main tabs: Home, Scan, Alerts, History, Profile. */
public class MainActivity extends AppCompatActivity {

    private BottomNavigationView nav;
    private LocalStore store;
    private String pendingScan;

    private final ActivityResultLauncher<String> notificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> { });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            goToLogin();
            return;
        }

        setContentView(R.layout.activity_main);
        store = new LocalStore(this);

        nav = findViewById(R.id.bottom_nav);
        nav.setOnItemSelectedListener(item -> {
            show(item.getItemId());
            return true;
        });

        if (savedInstanceState == null) {
            boolean openAlerts = "alerts".equals(getIntent().getStringExtra("open_tab"));
            nav.setSelectedItemId(openAlerts ? R.id.nav_alerts : R.id.nav_home);
            runFirstLaunchFlow();
        }
        syncDeviceInfo();
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void show(int itemId) {
        Fragment fragment;
        if (itemId == R.id.nav_scan) {
            ScanFragment scan = new ScanFragment();
            if (pendingScan != null) {
                Bundle args = new Bundle();
                args.putString("autostart", pendingScan);
                scan.setArguments(args);
                pendingScan = null;
            }
            fragment = scan;
        } else if (itemId == R.id.nav_alerts) {
            fragment = new AlertsFragment();
        } else if (itemId == R.id.nav_history) {
            fragment = new HistoryFragment();
        } else if (itemId == R.id.nav_profile) {
            fragment = new ProfileFragment();
        } else {
            fragment = new HomeFragment();
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    /** Called by the fragments to jump to another tab. */
    public void selectTab(int menuItemId) {
        nav.setSelectedItemId(menuItemId);
    }

    /** Opens the Scan tab and starts the given scan type straight away. */
    public void startScan(String scanType) {
        pendingScan = scanType;
        if (nav.getSelectedItemId() == R.id.nav_scan) {
            show(R.id.nav_scan);
        } else {
            nav.setSelectedItemId(R.id.nav_scan);
        }
    }

    // ---------------------------------------------------------------- first launch

    private void runFirstLaunchFlow() {
        if (store.isConsentAnswered()) {
            askNotificationPermission();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("Save your scans in the cloud?")
                .setMessage("Prohori can keep your scan history and security alerts in your own private "
                        + "Firebase account, so you can see them again later. It uploads scan summaries, "
                        + "apps rated Medium or higher, alerts and basic device info (model, Android version). "
                        + "It never uploads keystrokes, messages, files or passwords.\n\n"
                        + "If you say no, everything stays on this phone. You can change this in Settings.")
                .setCancelable(false)
                .setPositiveButton("Allow", (dialog, which) -> {
                    store.setCloudSync(true);
                    syncDeviceInfo();
                    askNotificationPermission();
                })
                .setNegativeButton("Not now", (dialog, which) -> {
                    store.setCloudSync(false);
                    askNotificationPermission();
                })
                .show();
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT < 33
                || store.isNotifPermissionAsked()
                || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        store.setNotifPermissionAsked(true);
        new MaterialAlertDialogBuilder(this)
                .setTitle("Allow notifications?")
                .setMessage("Prohori uses notifications to tell you about High and Critical findings. "
                        + "You can say no and still use every other feature.")
                .setPositiveButton("Continue", (dialog, which) ->
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS))
                .setNegativeButton("Not now", null)
                .show();
    }

    /** Registers this device and its push token, but only when the user allowed cloud sync. */
    private void syncDeviceInfo() {
        if (store == null || !store.isCloudSyncEnabled()) return;
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            String token = task.isSuccessful() ? task.getResult() : null;
            FirebaseManager.get().saveDeviceInfo(store.deviceId(), token);
        });
    }
}
