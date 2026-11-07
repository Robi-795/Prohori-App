package com.prohori.app.models;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Plain-data description of one installed app. The scanner fills it from Android APIs and
 * the RiskAnalyzer reads it. It has no Android dependency, so the rules can be unit tested.
 */
public class AppSnapshot {
    public String packageName = "";
    public String appName = "";
    public String versionName = "";
    public long versionCode;
    public long firstInstallTime;
    public long lastUpdateTime;
    /** Package name of the installer (for example com.android.vending), or null if unknown. */
    public String installer;
    public boolean systemApp;
    public boolean debuggable;
    public int targetSdk;

    public Set<String> requestedPermissions = new LinkedHashSet<>();
    public Set<String> grantedPermissions = new HashSet<>();

    // Currently switched on by the user in Android settings.
    public boolean accessibilityEnabled;
    public boolean inputMethodEnabled;
    public boolean notificationListenerEnabled;
    public boolean deviceAdminActive;

    // Declared in the app manifest (found by Deep Inspection), whether or not enabled.
    public boolean accessibilityDeclared;
    public boolean inputMethodDeclared;
    public boolean notificationListenerDeclared;
    public boolean deviceAdminDeclared;
}
