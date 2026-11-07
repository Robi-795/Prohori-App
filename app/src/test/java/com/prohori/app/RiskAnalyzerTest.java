package com.prohori.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.prohori.app.models.AppSnapshot;
import com.prohori.app.models.RiskLevel;
import com.prohori.app.models.RiskResult;
import com.prohori.app.security.RiskAnalyzer;

import org.junit.Test;

/** Unit tests for the transparent risk rules. They run on the JVM, no phone needed. */
public class RiskAnalyzerTest {

    private static final String P = "android.permission.";
    private static final String PLAY = "com.android.vending";

    /** Builds a snapshot with the given permissions, all granted. */
    private static AppSnapshot app(String installer, String... permissions) {
        AppSnapshot s = new AppSnapshot();
        s.packageName = "com.example.app";
        s.appName = "Example";
        s.versionName = "1.0";
        s.installer = installer;
        s.targetSdk = 34;
        for (String p : permissions) {
            s.requestedPermissions.add(P + p);
            s.grantedPermissions.add(P + p);
        }
        return s;
    }

    @Test
    public void appWithNoPermissionsIsSafe() {
        RiskResult r = RiskAnalyzer.analyze(app(PLAY));
        assertEquals(RiskLevel.SAFE, r.riskLevel);
        assertEquals(0, r.riskScore);
        assertFalse(r.reasons.isEmpty());
    }

    @Test
    public void internetPermissionAloneIsSafe() {
        RiskResult r = RiskAnalyzer.analyze(app(PLAY, "INTERNET"));
        assertEquals(RiskLevel.SAFE, r.riskLevel);
    }

    @Test
    public void messagingAppFromPlayStoreIsAtMostLow() {
        RiskResult r = RiskAnalyzer.analyze(app(PLAY, "INTERNET", "RECORD_AUDIO", "CAMERA",
                "READ_CONTACTS", "ACCESS_FINE_LOCATION", "READ_PHONE_STATE", "READ_EXTERNAL_STORAGE"));
        assertTrue("score was " + r.riskScore, r.riskLevel.severity <= RiskLevel.LOW.severity);
    }

    @Test
    public void sideloadedSmsAppIsHigh() {
        RiskResult r = RiskAnalyzer.analyze(app(null, "INTERNET", "READ_SMS", "RECEIVE_SMS",
                "SEND_SMS", "READ_CONTACTS"));
        assertEquals("score was " + r.riskScore, RiskLevel.HIGH, r.riskLevel);
        assertTrue(r.riskLevel.isAlertLevel());
    }

    @Test
    public void sideloadedAppWithEnabledAccessibilityIsHigh() {
        AppSnapshot s = app(null, "INTERNET");
        s.accessibilityEnabled = true;
        RiskResult r = RiskAnalyzer.analyze(s);
        assertEquals("score was " + r.riskScore, RiskLevel.HIGH, r.riskLevel);
    }

    @Test
    public void playStoreAppWithEnabledAccessibilityIsMedium() {
        AppSnapshot s = app(PLAY, "INTERNET");
        s.accessibilityEnabled = true;
        RiskResult r = RiskAnalyzer.analyze(s);
        assertEquals("score was " + r.riskScore, RiskLevel.MEDIUM, r.riskLevel);
    }

    @Test
    public void bankingTrojanPatternIsCritical() {
        AppSnapshot s = app(null, "INTERNET", "READ_SMS", "RECEIVE_SMS", "SYSTEM_ALERT_WINDOW");
        s.accessibilityEnabled = true;
        RiskResult r = RiskAnalyzer.analyze(s);
        assertEquals(RiskLevel.CRITICAL, r.riskLevel);
        assertEquals(100, r.riskScore);
    }

    @Test
    public void trustedStoreLowersScore() {
        AppSnapshot fromPlay = app(PLAY, "INTERNET", "READ_SMS", "READ_CONTACTS");
        AppSnapshot sideloaded = app(null, "INTERNET", "READ_SMS", "READ_CONTACTS");
        assertTrue(RiskAnalyzer.analyze(fromPlay).riskScore < RiskAnalyzer.analyze(sideloaded).riskScore);
    }

    @Test
    public void systemAppScoreIsReduced() {
        AppSnapshot user = app(PLAY, "READ_CALL_LOG", "READ_CONTACTS", "RECORD_AUDIO", "CAMERA");
        AppSnapshot system = app(null, "READ_CALL_LOG", "READ_CONTACTS", "RECORD_AUDIO", "CAMERA");
        system.systemApp = true;
        assertTrue(RiskAnalyzer.analyze(system).riskScore < RiskAnalyzer.analyze(user).riskScore);
    }

    @Test
    public void permissionNotGrantedCountsLessThanGranted() {
        AppSnapshot granted = app(null, "READ_SMS");
        AppSnapshot denied = app(null, "READ_SMS");
        denied.grantedPermissions.clear();
        assertTrue(RiskAnalyzer.analyze(denied).riskScore < RiskAnalyzer.analyze(granted).riskScore);
    }

    @Test
    public void enabledKeyboardFromUnknownSourceIsAlert() {
        AppSnapshot s = app(null, "INTERNET");
        s.inputMethodEnabled = true;
        RiskResult r = RiskAnalyzer.analyze(s);
        assertTrue("score was " + r.riskScore, r.riskLevel.isAlertLevel());
    }

    @Test
    public void everyResultHasReasonAndRecommendation() {
        RiskResult r = RiskAnalyzer.analyze(app(null, "READ_SMS"));
        assertFalse(r.reasons.isEmpty());
        assertFalse(r.recommendation.isEmpty());
        assertEquals("com.example.app", r.packageName);
        assertEquals("Example", r.appName);
    }

    @Test
    public void scoreStaysBetweenZeroAndHundred() {
        AppSnapshot s = app(null, "INTERNET", "READ_SMS", "RECEIVE_SMS", "SEND_SMS", "READ_CALL_LOG",
                "READ_CONTACTS", "RECORD_AUDIO", "CAMERA", "SYSTEM_ALERT_WINDOW", "REQUEST_INSTALL_PACKAGES");
        s.accessibilityEnabled = true;
        s.deviceAdminActive = true;
        s.inputMethodEnabled = true;
        s.notificationListenerEnabled = true;
        RiskResult r = RiskAnalyzer.analyze(s);
        assertTrue(r.riskScore >= 0 && r.riskScore <= 100);
    }

    @Test
    public void scoreThresholdsMapToLevels() {
        assertEquals(RiskLevel.SAFE, RiskLevel.fromScore(11));
        assertEquals(RiskLevel.LOW, RiskLevel.fromScore(12));
        assertEquals(RiskLevel.LOW, RiskLevel.fromScore(34));
        assertEquals(RiskLevel.MEDIUM, RiskLevel.fromScore(35));
        assertEquals(RiskLevel.MEDIUM, RiskLevel.fromScore(59));
        assertEquals(RiskLevel.HIGH, RiskLevel.fromScore(60));
        assertEquals(RiskLevel.HIGH, RiskLevel.fromScore(84));
        assertEquals(RiskLevel.CRITICAL, RiskLevel.fromScore(85));
        assertEquals(RiskLevel.CRITICAL, RiskLevel.fromScore(100));
    }

    private static final String[] MANY_PERMISSIONS = {"INTERNET", "READ_SMS", "RECEIVE_SMS", "SEND_SMS",
            "READ_CALL_LOG", "WRITE_CALL_LOG", "CALL_PHONE", "READ_PHONE_STATE", "RECORD_AUDIO", "CAMERA",
            "ACCESS_FINE_LOCATION", "ACCESS_BACKGROUND_LOCATION", "READ_CONTACTS", "WRITE_CONTACTS",
            "GET_ACCOUNTS", "READ_CALENDAR", "BODY_SENSORS", "SYSTEM_ALERT_WINDOW",
            "REQUEST_INSTALL_PACKAGES", "PACKAGE_USAGE_STATS"};

    @Test
    public void systemAppWithManyPermissionsIsNeverAboveMedium() {
        AppSnapshot s = app(null, MANY_PERMISSIONS);
        s.systemApp = true;
        RiskResult r = RiskAnalyzer.analyze(s);
        assertEquals("score was " + r.riskScore, RiskLevel.MEDIUM, r.riskLevel);
        assertEquals(RiskAnalyzer.SYSTEM_APP_MAX_SCORE, r.riskScore);
    }

    @Test
    public void userAppWithSamePermissionsIsStillCritical() {
        RiskResult r = RiskAnalyzer.analyze(app(null, MANY_PERMISSIONS));
        assertEquals(RiskLevel.CRITICAL, r.riskLevel);
    }
}