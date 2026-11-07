package com.prohori.app.security;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The permission table behind the risk score. Every entry has a weight (points added to the
 * score) and a plain-language explanation that is shown to the user. Nothing is hidden.
 *
 * A "special" permission is one where Android does not reliably report a granted flag
 * (for example draw-over-other-apps), so the full weight is used when it is requested.
 */
public final class PermissionRules {

    public static final class Rule {
        public final String label;
        public final int weight;
        public final boolean special;
        public final String explanation;

        Rule(String label, int weight, boolean special, String explanation) {
            this.label = label;
            this.weight = weight;
            this.special = special;
            this.explanation = explanation;
        }
    }

    private static final String P = "android.permission.";
    private static final Map<String, Rule> RULES = new LinkedHashMap<>();

    private static void add(String name, String label, int weight, boolean special, String explanation) {
        RULES.put(P + name, new Rule(label, weight, special, explanation));
    }

    static {
        // Messages and calls
        add("READ_SMS", "Read text messages", 20, false,
                "Can read all SMS, including one-time passwords (OTP) and bank messages.");
        add("RECEIVE_SMS", "Receive text messages", 12, false,
                "Can see incoming SMS as they arrive, which includes verification codes.");
        add("SEND_SMS", "Send text messages", 15, false,
                "Can send SMS without you typing them, which may cost money or spread bad links.");
        add("READ_CALL_LOG", "Read call log", 12, false,
                "Can see who you called and who called you.");
        add("WRITE_CALL_LOG", "Change call log", 6, false,
                "Can add or erase entries in your call history.");
        add("PROCESS_OUTGOING_CALLS", "See outgoing calls", 10, false,
                "Can see numbers you dial and redirect or end calls.");
        add("CALL_PHONE", "Make phone calls", 5, false,
                "Can start calls without opening the dialer.");
        add("ANSWER_PHONE_CALLS", "Answer phone calls", 6, false,
                "Can answer incoming calls automatically.");
        add("READ_PHONE_STATE", "Read phone state", 5, false,
                "Can read phone status and device identifiers.");
        add("READ_PHONE_NUMBERS", "Read phone number", 4, false,
                "Can read the phone number of this device.");

        // Sensors and location
        add("RECORD_AUDIO", "Record audio", 10, false,
                "Can use the microphone to record sound around you.");
        add("CAMERA", "Use camera", 8, false,
                "Can take photos and record video.");
        add("ACCESS_FINE_LOCATION", "Precise location", 8, false,
                "Can find your exact location.");
        add("ACCESS_COARSE_LOCATION", "Approximate location", 3, false,
                "Can find your general area.");
        add("ACCESS_BACKGROUND_LOCATION", "Location in background", 12, false,
                "Can track your location even when the app is closed.");
        add("BODY_SENSORS", "Body sensors", 6, false,
                "Can read health sensor data such as heart rate.");

        // Personal data
        add("READ_CONTACTS", "Read contacts", 8, false,
                "Can read your whole contact list.");
        add("WRITE_CONTACTS", "Change contacts", 4, false,
                "Can add, edit or delete contacts.");
        add("GET_ACCOUNTS", "See accounts on device", 5, false,
                "Can list the accounts (email, social) set up on this phone.");
        add("READ_CALENDAR", "Read calendar", 5, false,
                "Can read your calendar events.");
        add("WRITE_CALENDAR", "Change calendar", 3, false,
                "Can add or change calendar events.");

        // Storage
        add("READ_EXTERNAL_STORAGE", "Read shared storage", 3, false,
                "Can read files in shared storage.");
        add("WRITE_EXTERNAL_STORAGE", "Write shared storage", 3, false,
                "Can create, change and delete files in shared storage.");
        add("READ_MEDIA_IMAGES", "Read photos", 3, false,
                "Can read photos stored on the device.");
        add("READ_MEDIA_VIDEO", "Read videos", 3, false,
                "Can read videos stored on the device.");
        add("MANAGE_EXTERNAL_STORAGE", "Access all files", 10, true,
                "Can read and change almost every file on shared storage.");

        // Special access
        add("SYSTEM_ALERT_WINDOW", "Draw over other apps", 15, true,
                "Can show windows on top of other apps. Malware uses this to fake login screens.");
        add("REQUEST_INSTALL_PACKAGES", "Install other apps", 10, true,
                "Can ask to install more apps. Droppers use this to bring in other malware.");
        add("REQUEST_DELETE_PACKAGES", "Remove other apps", 5, false,
                "Can ask to uninstall apps.");
        add("PACKAGE_USAGE_STATS", "See app usage", 8, true,
                "Can see which apps you use and for how long.");
        add("WRITE_SETTINGS", "Change system settings", 5, true,
                "Can change system settings such as brightness and ringtone.");
        add("READ_LOGS", "Read system logs", 12, false,
                "Can read system logs, which may contain information from other apps.");
        add("DISABLE_KEYGUARD", "Disable screen lock", 5, false,
                "Can switch off the keyguard when the phone is unlocked.");
        add("QUERY_ALL_PACKAGES", "See all installed apps", 4, false,
                "Can list every app installed on the phone.");
        add("RECEIVE_BOOT_COMPLETED", "Start at boot", 2, false,
                "Starts automatically when the phone restarts.");
    }

    private PermissionRules() { }

    /** Returns the rule for a full permission name, or null when it is not a tracked one. */
    public static Rule get(String permission) {
        return RULES.get(permission);
    }

    /** Short display name, e.g. android.permission.CAMERA becomes CAMERA. */
    public static String shortName(String permission) {
        int dot = permission.lastIndexOf('.');
        return dot >= 0 ? permission.substring(dot + 1) : permission;
    }
}
