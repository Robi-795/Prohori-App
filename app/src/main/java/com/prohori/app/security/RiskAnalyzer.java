package com.prohori.app.security;

import com.prohori.app.models.AppSnapshot;
import com.prohori.app.models.RiskLevel;
import com.prohori.app.models.RiskResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Rule based risk engine. The score is the sum of visible rules:
 *
 *  1. Sensitive permissions (weights in PermissionRules). A runtime permission that is
 *     requested but not granted counts for half.
 *  2. Special services that are switched on (accessibility, keyboard, notification
 *     listener, device admin). These are the strongest signals because they let an app
 *     see or control what happens on screen.
 *  3. Dangerous combinations, for example "can read SMS" together with internet access.
 *  4. App information: sideloaded, debuggable, very old target SDK.
 *  5. Adjustments: apps from a recognised store score 40% lower, system apps 70% lower
 *     and never above Medium (pre-installed apps hold many permissions legitimately).
 *
 * The result is clamped to 0-100 and mapped to a level (see RiskLevel). A high score means
 * "worth a closer look", not "this app is malware".
 */
public final class RiskAnalyzer {

    public static final double TRUSTED_STORE_FACTOR = 0.6;
    public static final double SYSTEM_APP_FACTOR = 0.3;
    public static final double NOT_GRANTED_FACTOR = 0.5;
    /** System apps never go above Medium (one point below the High threshold). */
    public static final int SYSTEM_APP_MAX_SCORE = RiskLevel.HIGH_MIN - 1;

    private static final String P = "android.permission.";
    private static final String INTERNET = P + "INTERNET";

    private static final class Reason {
        final int points;
        final String text;

        Reason(int points, String text) {
            this.points = points;
            this.text = text;
        }
    }

    private RiskAnalyzer() { }

    public static RiskResult analyze(AppSnapshot a) {
        List<Reason> reasons = new ArrayList<>();
        List<String> sensitive = new ArrayList<>();
        double score = 0;

        boolean trustedStore = !a.systemApp && TrustedSources.isTrusted(a.installer);
        boolean sideloaded = !a.systemApp && !trustedStore;
        boolean internet = a.requestedPermissions.contains(INTERNET);

        // 1. Sensitive permissions
        for (String permission : a.requestedPermissions) {
            PermissionRules.Rule rule = PermissionRules.get(permission);
            if (rule == null) continue;
            boolean granted = a.grantedPermissions.contains(permission);
            double weight = rule.weight;
            if (!rule.special && !granted) weight *= NOT_GRANTED_FACTOR;
            score += weight;
            String state = rule.special ? "requested" : (granted ? "granted" : "not granted");
            sensitive.add(rule.label + " (" + state + ")");
            if (rule.weight >= 8) {
                reasons.add(new Reason((int) Math.round(weight),
                        rule.label + " (" + state + "): " + rule.explanation));
            }
        }

        // 2. Special services
        if (a.accessibilityEnabled) {
            score += add(reasons, 40, "An accessibility service from this app is switched ON. "
                    + "Accessibility services can read what is on screen and what is typed, "
                    + "so only trusted apps should have this.");
        } else if (a.accessibilityDeclared) {
            score += add(reasons, 8, "Declares an accessibility service (currently not switched on).");
        }
        if (a.inputMethodEnabled) {
            score += add(reasons, 25, "This app is an ENABLED keyboard. A keyboard can see everything "
                    + "you type, including passwords. Make sure you trust it.");
        } else if (a.inputMethodDeclared) {
            score += add(reasons, 3, "Includes a keyboard (currently not enabled).");
        }
        if (a.notificationListenerEnabled) {
            score += add(reasons, 25, "This app can read all your notifications (access is ON), "
                    + "including message previews and verification codes.");
        } else if (a.notificationListenerDeclared) {
            score += add(reasons, 4, "Declares notification access (currently not switched on).");
        }
        if (a.deviceAdminActive) {
            score += add(reasons, 25, "This app is an ACTIVE device administrator. It can lock the "
                    + "device and make itself hard to uninstall.");
        } else if (a.deviceAdminDeclared) {
            score += add(reasons, 6, "Declares a device administrator component (currently not active).");
        }

        // 3. Dangerous combinations
        boolean readsSms = has(a, "READ_SMS") || has(a, "RECEIVE_SMS");
        boolean overlay = has(a, "SYSTEM_ALERT_WINDOW");
        boolean accessibility = a.accessibilityEnabled || a.accessibilityDeclared;
        if (readsSms && internet) {
            score += add(reasons, 12, "Combination: can see incoming SMS and has internet access, "
                    + "a pattern used by apps that steal one-time passwords.");
        }
        if (a.accessibilityEnabled && internet) {
            score += add(reasons, 20, "Combination: accessibility is ON and the app has internet access, "
                    + "so what it sees on screen could be sent off the phone.");
        }
        if (a.inputMethodEnabled && internet) {
            score += add(reasons, 10, "Combination: an enabled keyboard with internet access, "
                    + "so typed text could be sent off the phone.");
        }
        if (overlay && accessibility) {
            score += add(reasons, 15, "Combination: draw-over-other-apps plus an accessibility service, "
                    + "a pattern used by banking trojans.");
        }
        if (has(a, "RECORD_AUDIO") && has(a, "CAMERA") && internet) {
            score += add(reasons, 6, "Combination: microphone, camera and internet access together.");
        }
        if (has(a, "READ_CONTACTS") && has(a, "READ_SMS") && has(a, "READ_CALL_LOG")) {
            score += add(reasons, 10, "Combination: contacts, messages and call log together "
                    + "give a full picture of who you talk to.");
        }
        if (has(a, "ACCESS_BACKGROUND_LOCATION") && internet) {
            score += add(reasons, 6, "Combination: location tracking in the background plus internet access.");
        }
        if (sideloaded && has(a, "REQUEST_INSTALL_PACKAGES")) {
            score += add(reasons, 10, "Combination: installed outside a store and able to install other apps.");
        }
        if (sideloaded && a.deviceAdminActive) {
            score += add(reasons, 15, "Combination: installed outside a store and an active device administrator.");
        }
        if (sideloaded && a.inputMethodEnabled) {
            score += add(reasons, 20, "Combination: a keyboard installed outside a store.");
        }

        // 4. App information
        if (sideloaded) {
            score += add(reasons, 10, "Installed from outside a recognised app store ("
                    + TrustedSources.label(a.installer, false) + ").");
        }
        if (!a.systemApp && a.debuggable) {
            score += add(reasons, 8, "The app is marked debuggable, which is unusual for a released app.");
        }
        if (!a.systemApp && a.targetSdk > 0 && a.targetSdk < 23) {
            score += add(reasons, 6, "Built for an old Android version (API " + a.targetSdk
                    + "), so its permissions are granted at install time.");
        }

        // 5. Adjustments
        if (trustedStore && score > 0) {
            score *= TRUSTED_STORE_FACTOR;
            reasons.add(new Reason(0, "Installed from a recognised app store, so the score is reduced by 40%."));
        }
        if (a.systemApp && score > 0) {
            score *= SYSTEM_APP_FACTOR;
            reasons.add(new Reason(0, "Pre-installed system app, so the score is reduced by 70%."));
            if (score > SYSTEM_APP_MAX_SCORE) {
                score = SYSTEM_APP_MAX_SCORE;
                reasons.add(new Reason(0, "Pre-installed apps legitimately hold many permissions and cannot be "
                        + "chosen by you, so they are never rated above Medium. This is for your awareness only."));
            }
        }

        int finalScore = (int) Math.max(0, Math.min(100, Math.round(score)));
        RiskLevel level = RiskLevel.fromScore(finalScore);

        Collections.sort(reasons, new Comparator<Reason>() {
            @Override
            public int compare(Reason x, Reason y) {
                return y.points - x.points;
            }
        });

        RiskResult result = new RiskResult();
        result.appName = a.appName;
        result.packageName = a.packageName;
        result.versionName = a.versionName;
        result.systemApp = a.systemApp;
        result.installSource = TrustedSources.label(a.installer, a.systemApp);
        result.riskLevel = level;
        result.riskScore = finalScore;
        result.sensitivePermissions.addAll(sensitive);
        for (Reason r : reasons) {
            result.reasons.add(r.text);
        }
        if (result.reasons.isEmpty()) {
            result.reasons.add("No sensitive permissions or other risk indicators were found.");
        }
        result.recommendation = recommendation(level, a);
        return result;
    }

    private static int add(List<Reason> reasons, int points, String text) {
        reasons.add(new Reason(points, text));
        return points;
    }

    private static boolean has(AppSnapshot a, String shortName) {
        return a.requestedPermissions.contains(P + shortName);
    }

    private static String recommendation(RiskLevel level, AppSnapshot a) {
        StringBuilder sb = new StringBuilder();
        switch (level) {
            case CRITICAL:
                sb.append("Review this app now. If you do not recognise it or no longer need it, "
                        + "uninstall it from Android Settings and change passwords you entered on this phone.");
                break;
            case HIGH:
                sb.append("Check that you trust this app. Open its Android app info, remove permissions "
                        + "it does not need, and uninstall it if you are unsure.");
                break;
            case MEDIUM:
                sb.append("Review its permissions in Android app info and remove the ones you do not need "
                        + "for the features you use.");
                break;
            case LOW:
                sb.append("No action needed. You can review its permissions occasionally.");
                break;
            default:
                sb.append("No known risk indicators were found.");
                break;
        }
        if (a.accessibilityEnabled) {
            sb.append(" To switch off its accessibility service: Settings > Accessibility.");
        }
        if (a.inputMethodEnabled && !a.systemApp) {
            sb.append(" To stop using this keyboard: Settings > System > Keyboard.");
        }
        if (a.notificationListenerEnabled) {
            sb.append(" To remove its notification access: Settings > Notifications > Device and app notifications.");
        }
        if (a.deviceAdminActive) {
            sb.append(" To remove device admin rights: Settings > Security > Device admin apps.");
        }
        return sb.toString();
    }
}