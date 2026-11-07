package com.prohori.app.scanner;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.view.accessibility.AccessibilityManager;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.core.app.NotificationManagerCompat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Which apps currently have a powerful special service switched on. These are public
 * Android APIs that need no extra permission.
 */
public final class EnabledServices {

    public final Set<String> accessibility = new HashSet<>();
    public final Set<String> inputMethods = new HashSet<>();
    public final Set<String> notificationListeners = new HashSet<>();
    public final Set<String> deviceAdmins = new HashSet<>();

    private EnabledServices() { }

    public static EnabledServices read(Context context) {
        EnabledServices result = new EnabledServices();

        try {
            AccessibilityManager am =
                    (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
            if (am != null) {
                List<AccessibilityServiceInfo> list =
                        am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
                for (AccessibilityServiceInfo info : list) {
                    if (info.getResolveInfo() != null && info.getResolveInfo().serviceInfo != null) {
                        result.accessibility.add(info.getResolveInfo().serviceInfo.packageName);
                    }
                }
            }
        } catch (Exception ignored) {
            // If Android refuses, we simply report nothing for this signal.
        }

        try {
            InputMethodManager imm =
                    (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                for (InputMethodInfo info : imm.getEnabledInputMethodList()) {
                    result.inputMethods.add(info.getPackageName());
                }
            }
        } catch (Exception ignored) {
        }

        try {
            result.notificationListeners.addAll(NotificationManagerCompat.getEnabledListenerPackages(context));
        } catch (Exception ignored) {
        }

        try {
            DevicePolicyManager dpm =
                    (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            if (dpm != null) {
                List<ComponentName> admins = dpm.getActiveAdmins();
                if (admins != null) {
                    for (ComponentName c : admins) {
                        result.deviceAdmins.add(c.getPackageName());
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return result;
    }
}
