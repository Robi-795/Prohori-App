package com.prohori.app.scanner;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;

import com.prohori.app.models.AppSnapshot;
import com.prohori.app.models.RiskResult;
import com.prohori.app.models.ScanReport;
import com.prohori.app.security.RiskAnalyzer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Reads information about installed apps through PackageManager (the normal, supported way)
 * and hands it to the RiskAnalyzer. It never changes, disables or removes any app.
 *
 * Call these methods from a background thread: a full scan can take a few seconds.
 */
public final class SecurityScanner {

    public static final String QUICK_SCAN = "Quick Scan";
    public static final String DEEP_INSPECTION = "Deep Inspection";

    private static final String BIND_ACCESSIBILITY = "android.permission.BIND_ACCESSIBILITY_SERVICE";
    private static final String BIND_INPUT_METHOD = "android.permission.BIND_INPUT_METHOD";
    private static final String BIND_NOTIFICATION_LISTENER = "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE";
    private static final String BIND_DEVICE_ADMIN = "android.permission.BIND_DEVICE_ADMIN";

    private SecurityScanner() { }

    /**
     * Quick Scan: apps you installed (plus system apps if includeSystem is true).
     * Deep Inspection: all apps, and also checks the special services each app declares.
     */
    public static ScanReport scan(Context context, String scanType, boolean includeSystem) {
        boolean deep = DEEP_INSPECTION.equals(scanType);
        List<RiskResult> results = analyzeAll(context, deep || includeSystem, deep);
        return ScanReport.fromResults(scanType, System.currentTimeMillis(), results);
    }

    /** Analyses every visible app and returns all results (including Safe), highest risk first. */
    public static List<RiskResult> analyzeAll(Context context, boolean includeSystem, boolean deep) {
        Context app = context.getApplicationContext();
        PackageManager pm = app.getPackageManager();
        EnabledServices enabled = EnabledServices.read(app);

        List<RiskResult> results = new ArrayList<>();
        for (PackageInfo base : installedPackages(pm)) {
            if (base.applicationInfo == null) continue;
            if (!includeSystem && isSystem(base.applicationInfo)) continue;
            PackageInfo info = base;
            if (deep) {
                PackageInfo full = packageInfo(pm, base.packageName, true);
                if (full != null) info = full;
            }
            results.add(RiskAnalyzer.analyze(toSnapshot(pm, info, enabled, deep)));
        }

        Collections.sort(results, new Comparator<RiskResult>() {
            @Override
            public int compare(RiskResult a, RiskResult b) {
                if (a.riskLevel.severity != b.riskLevel.severity) {
                    return b.riskLevel.severity - a.riskLevel.severity;
                }
                if (a.riskScore != b.riskScore) {
                    return b.riskScore - a.riskScore;
                }
                return a.appName.compareToIgnoreCase(b.appName);
            }
        });
        return results;
    }

    /** Full (deep) snapshot of one app, or null if it is no longer installed or not visible. */
    public static AppSnapshot snapshotFor(Context context, String packageName) {
        Context app = context.getApplicationContext();
        PackageManager pm = app.getPackageManager();
        PackageInfo info = packageInfo(pm, packageName, true);
        if (info == null || info.applicationInfo == null) return null;
        return toSnapshot(pm, info, EnabledServices.read(app), true);
    }

    // ---------------------------------------------------------------------------------

    @SuppressWarnings("deprecation")
    private static List<PackageInfo> installedPackages(PackageManager pm) {
        if (Build.VERSION.SDK_INT >= 33) {
            return pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS));
        }
        return pm.getInstalledPackages(PackageManager.GET_PERMISSIONS);
    }

    @SuppressWarnings("deprecation")
    private static PackageInfo packageInfo(PackageManager pm, String packageName, boolean deep) {
        int flags = PackageManager.GET_PERMISSIONS;
        if (deep) {
            flags |= PackageManager.GET_SERVICES | PackageManager.GET_RECEIVERS;
        }
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                return pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags));
            }
            return pm.getPackageInfo(packageName, flags);
        } catch (PackageManager.NameNotFoundException | RuntimeException e) {
            return null;
        }
    }

    private static boolean isSystem(ApplicationInfo ai) {
        return (ai.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
    }

    @SuppressWarnings("deprecation")
    private static String installerOf(PackageManager pm, String packageName) {
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                return pm.getInstallSourceInfo(packageName).getInstallingPackageName();
            }
            return pm.getInstallerPackageName(packageName);
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    private static AppSnapshot toSnapshot(PackageManager pm, PackageInfo pi, EnabledServices enabled, boolean deep) {
        ApplicationInfo ai = pi.applicationInfo;
        AppSnapshot s = new AppSnapshot();
        s.packageName = pi.packageName;
        CharSequence label = pm.getApplicationLabel(ai);
        s.appName = label != null ? label.toString() : pi.packageName;
        s.versionName = pi.versionName != null ? pi.versionName : "unknown";
        s.versionCode = Build.VERSION.SDK_INT >= 28 ? pi.getLongVersionCode() : pi.versionCode;
        s.firstInstallTime = pi.firstInstallTime;
        s.lastUpdateTime = pi.lastUpdateTime;
        s.systemApp = isSystem(ai);
        s.debuggable = (ai.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        s.targetSdk = ai.targetSdkVersion;
        s.installer = installerOf(pm, pi.packageName);

        if (pi.requestedPermissions != null) {
            for (int i = 0; i < pi.requestedPermissions.length; i++) {
                String permission = pi.requestedPermissions[i];
                s.requestedPermissions.add(permission);
                boolean granted = pi.requestedPermissionsFlags == null
                        || i >= pi.requestedPermissionsFlags.length
                        || (pi.requestedPermissionsFlags[i] & PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0;
                if (granted) {
                    s.grantedPermissions.add(permission);
                }
            }
        }

        s.accessibilityEnabled = enabled.accessibility.contains(pi.packageName);
        s.inputMethodEnabled = enabled.inputMethods.contains(pi.packageName);
        s.notificationListenerEnabled = enabled.notificationListeners.contains(pi.packageName);
        s.deviceAdminActive = enabled.deviceAdmins.contains(pi.packageName);

        if (deep) {
            if (pi.services != null) {
                for (ServiceInfo si : pi.services) {
                    if (BIND_ACCESSIBILITY.equals(si.permission)) s.accessibilityDeclared = true;
                    if (BIND_INPUT_METHOD.equals(si.permission)) s.inputMethodDeclared = true;
                    if (BIND_NOTIFICATION_LISTENER.equals(si.permission)) s.notificationListenerDeclared = true;
                }
            }
            if (pi.receivers != null) {
                for (ActivityInfo ri : pi.receivers) {
                    if (BIND_DEVICE_ADMIN.equals(ri.permission)) s.deviceAdminDeclared = true;
                }
            }
        }
        return s;
    }
}
