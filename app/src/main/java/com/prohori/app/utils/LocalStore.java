package com.prohori.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.prohori.app.models.ScanReport;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

/**
 * Small on-device storage (SharedPreferences, private to the app). It holds settings and the
 * summary of the last scan. It never stores passwords: Firebase keeps the login session.
 */
public class LocalStore {

    private static final String PREFS = "prohori_prefs";
    private static final String KEY_CLOUD = "cloud_consent";        // -1 not asked, 0 no, 1 yes
    private static final String KEY_NOTIFICATIONS = "notifications";
    private static final String KEY_SYSTEM_APPS = "include_system_apps";
    private static final String KEY_NOTIF_ASKED = "notif_permission_asked";
    private static final String KEY_DEVICE_ID = "device_id";
    private static final String KEY_LAST_SCAN = "last_scan";

    private final SharedPreferences prefs;

    public LocalStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ---- Cloud sync consent ----
    public boolean isConsentAnswered() {
        return prefs.getInt(KEY_CLOUD, -1) != -1;
    }

    public boolean isCloudSyncEnabled() {
        return prefs.getInt(KEY_CLOUD, -1) == 1;
    }

    public void setCloudSync(boolean enabled) {
        prefs.edit().putInt(KEY_CLOUD, enabled ? 1 : 0).apply();
    }

    // ---- Settings ----
    public boolean isNotificationsEnabled() {
        return prefs.getBoolean(KEY_NOTIFICATIONS, true);
    }

    public void setNotificationsEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply();
    }

    public boolean isIncludeSystemApps() {
        return prefs.getBoolean(KEY_SYSTEM_APPS, false);
    }

    public void setIncludeSystemApps(boolean include) {
        prefs.edit().putBoolean(KEY_SYSTEM_APPS, include).apply();
    }

    public boolean isNotifPermissionAsked() {
        return prefs.getBoolean(KEY_NOTIF_ASKED, false);
    }

    public void setNotifPermissionAsked(boolean asked) {
        prefs.edit().putBoolean(KEY_NOTIF_ASKED, asked).apply();
    }

    /** Random id for this install, used to name the device record in the cloud. */
    public String deviceId() {
        String id = prefs.getString(KEY_DEVICE_ID, null);
        if (id == null) {
            id = UUID.randomUUID().toString();
            prefs.edit().putString(KEY_DEVICE_ID, id).apply();
        }
        return id;
    }

    // ---- Last scan summary (for the dashboard) ----
    public void saveLastScan(ScanReport r) {
        try {
            JSONObject o = new JSONObject();
            o.put("scanType", r.scanType);
            o.put("scanDate", r.scanDate);
            o.put("total", r.totalApps);
            o.put("critical", r.critical);
            o.put("high", r.high);
            o.put("medium", r.medium);
            o.put("low", r.low);
            o.put("safe", r.safe);
            prefs.edit().putString(KEY_LAST_SCAN, o.toString()).apply();
        } catch (JSONException ignored) {
        }
    }

    /** Returns the last scan summary (without the findings list), or null if none yet. */
    public ScanReport loadLastScan() {
        String json = prefs.getString(KEY_LAST_SCAN, null);
        if (json == null) return null;
        try {
            JSONObject o = new JSONObject(json);
            ScanReport r = new ScanReport();
            r.scanType = o.optString("scanType");
            r.scanDate = o.optLong("scanDate");
            r.totalApps = o.optInt("total");
            r.critical = o.optInt("critical");
            r.high = o.optInt("high");
            r.medium = o.optInt("medium");
            r.low = o.optInt("low");
            r.safe = o.optInt("safe");
            return r;
        } catch (JSONException e) {
            return null;
        }
    }

    /** Used at logout so the next person who signs in starts clean. */
    public void clearAll() {
        prefs.edit().clear().apply();
    }
}
