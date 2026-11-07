package com.prohori.app.firebase;

import android.os.Build;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.prohori.app.models.RiskLevel;
import com.prohori.app.models.RiskResult;
import com.prohori.app.models.ScanReport;
import com.prohori.app.models.SecurityAlert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * All Firebase Authentication and Firestore work in one place.
 *
 * Every document carries the owner's userId. The Firestore security rules (firebase/firestore.rules)
 * only let a signed-in user read or change documents where userId equals their own uid.
 * Queries only filter by userId and are sorted on the phone, so no composite index is needed.
 */
public final class FirebaseManager {

    public interface Callback<T> {
        void onSuccess(T result);

        void onError(String message);
    }

    public static final String USERS = "users";
    public static final String SCANS = "scan_history";
    public static final String ALERTS = "security_alerts";
    public static final String DEVICES = "device_information";

    private static final int MAX_STORED_FINDINGS = 100;
    private static final int MAX_BATCH = 400;

    private static FirebaseManager instance;

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private FirebaseManager() { }

    public static synchronized FirebaseManager get() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    // ---------------------------------------------------------------- account

    public FirebaseUser currentUser() {
        return auth.getCurrentUser();
    }

    public String uid() {
        FirebaseUser user = auth.getCurrentUser();
        return user == null ? null : user.getUid();
    }

    /** Saves name and email for a new account. Passwords are never stored here. */
    public void saveUserDoc(String uid, String name, String email) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("email", email);
        data.put("createdAt", FieldValue.serverTimestamp());
        db.collection(USERS).document(uid).set(data, SetOptions.merge());
    }

    /** Saves basic device info and the push token. Only call this when the user allowed cloud sync. */
    public void saveDeviceInfo(String deviceId, String fcmToken) {
        String uid = uid();
        if (uid == null) return;
        Map<String, Object> data = new HashMap<>();
        data.put("userId", uid);
        data.put("deviceName", Build.MANUFACTURER + " " + Build.MODEL);
        data.put("androidVersion", Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
        data.put("lastSync", FieldValue.serverTimestamp());
        if (fcmToken != null) {
            data.put("fcmToken", fcmToken);
        }
        db.collection(DEVICES).document(uid + "_" + deviceId).set(data, SetOptions.merge());
    }

    // ---------------------------------------------------------------- scan history

    public void saveScan(ScanReport report, final Callback<String> callback) {
        String uid = uid();
        if (uid == null) {
            callback.onError("You are not signed in.");
            return;
        }
        Map<String, Object> summary = new HashMap<>();
        summary.put("critical", report.critical);
        summary.put("high", report.high);
        summary.put("medium", report.medium);
        summary.put("low", report.low);
        summary.put("safe", report.safe);

        List<Map<String, Object>> findings = new ArrayList<>();
        for (RiskResult r : report.findings) {
            if (!r.riskLevel.needsReview()) continue;
            if (findings.size() >= MAX_STORED_FINDINGS) break;
            Map<String, Object> f = new HashMap<>();
            f.put("appName", r.appName);
            f.put("packageName", r.packageName);
            f.put("riskLevel", r.riskLevel.label);
            f.put("riskScore", r.riskScore);
            f.put("reasons", new ArrayList<>(r.reasons.subList(0, Math.min(5, r.reasons.size()))));
            findings.add(f);
        }

        Map<String, Object> doc = new HashMap<>();
        doc.put("userId", uid);
        doc.put("scanType", report.scanType);
        doc.put("scanDate", new Date(report.scanDate));
        doc.put("totalAppsScanned", report.totalApps);
        doc.put("riskyApps", report.riskyApps());
        doc.put("riskSummary", summary);
        doc.put("findings", findings);

        db.collection(SCANS).add(doc)
                .addOnSuccessListener(ref -> callback.onSuccess(ref.getId()))
                .addOnFailureListener(e -> callback.onError(message(e)));
    }

    @SuppressWarnings("unchecked")
    public void loadScans(final Callback<List<ScanReport>> callback) {
        String uid = uid();
        if (uid == null) {
            callback.onError("You are not signed in.");
            return;
        }
        db.collection(SCANS).whereEqualTo("userId", uid).get()
                .addOnSuccessListener(snapshot -> {
                    List<ScanReport> list = new ArrayList<>();
                    for (DocumentSnapshot d : snapshot.getDocuments()) {
                        ScanReport r = new ScanReport();
                        r.id = d.getId();
                        r.partial = true;
                        r.scanType = d.getString("scanType") == null ? "Scan" : d.getString("scanType");
                        Date date = d.getDate("scanDate");
                        r.scanDate = date == null ? 0 : date.getTime();
                        r.totalApps = intOf(d.get("totalAppsScanned"));
                        Object summaryObj = d.get("riskSummary");
                        if (summaryObj instanceof Map) {
                            Map<String, Object> summary = (Map<String, Object>) summaryObj;
                            r.critical = intOf(summary.get("critical"));
                            r.high = intOf(summary.get("high"));
                            r.medium = intOf(summary.get("medium"));
                            r.low = intOf(summary.get("low"));
                            r.safe = intOf(summary.get("safe"));
                        }
                        Object findingsObj = d.get("findings");
                        if (findingsObj instanceof List) {
                            for (Object o : (List<Object>) findingsObj) {
                                if (!(o instanceof Map)) continue;
                                Map<String, Object> f = (Map<String, Object>) o;
                                RiskResult x = new RiskResult();
                                x.appName = stringOf(f.get("appName"));
                                x.packageName = stringOf(f.get("packageName"));
                                x.riskLevel = RiskLevel.fromLabel(stringOf(f.get("riskLevel")));
                                x.riskScore = intOf(f.get("riskScore"));
                                Object reasons = f.get("reasons");
                                if (reasons instanceof List) {
                                    for (Object reason : (List<Object>) reasons) {
                                        x.reasons.add(stringOf(reason));
                                    }
                                }
                                r.findings.add(x);
                            }
                        }
                        list.add(r);
                    }
                    Collections.sort(list, (a, b) -> Long.compare(b.scanDate, a.scanDate));
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> callback.onError(message(e)));
    }

    public void deleteScan(String scanId, final Callback<Void> callback) {
        db.collection(SCANS).document(scanId).delete()
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(message(e)));
    }

    // ---------------------------------------------------------------- alerts

    /**
     * Creates alerts for Critical and High findings. The alert id is "uid_packageName", so a
     * repeated scan does not create duplicates. A new alert is created only when the app has no
     * alert yet or its risk level changed. Dismissed alerts stay dismissed while the level is the same.
     * The callback receives the number of new alerts.
     */
    public void syncAlerts(List<RiskResult> findings, final Callback<Integer> callback) {
        final String uid = uid();
        if (uid == null) {
            callback.onError("You are not signed in.");
            return;
        }
        final List<RiskResult> candidates = new ArrayList<>();
        for (RiskResult r : findings) {
            if (r.riskLevel.isAlertLevel()) candidates.add(r);
        }
        if (candidates.isEmpty()) {
            callback.onSuccess(0);
            return;
        }
        db.collection(ALERTS).whereEqualTo("userId", uid).get()
                .addOnSuccessListener(snapshot -> {
                    Map<String, String> existing = new HashMap<>();
                    for (DocumentSnapshot d : snapshot.getDocuments()) {
                        existing.put(d.getString("packageName"), d.getString("riskLevel"));
                    }
                    WriteBatch batch = db.batch();
                    int created = 0;
                    for (RiskResult r : candidates) {
                        if (created >= MAX_BATCH) break;
                        String previous = existing.get(r.packageName);
                        if (previous != null && previous.equals(r.riskLevel.label)) continue;
                        batch.set(db.collection(ALERTS).document(uid + "_" + r.packageName), alertMap(uid, r));
                        created++;
                    }
                    if (created == 0) {
                        callback.onSuccess(0);
                        return;
                    }
                    final int total = created;
                    batch.commit()
                            .addOnSuccessListener(v -> callback.onSuccess(total))
                            .addOnFailureListener(e -> callback.onError(message(e)));
                })
                .addOnFailureListener(e -> callback.onError(message(e)));
    }

    private static Map<String, Object> alertMap(String uid, RiskResult r) {
        StringBuilder description = new StringBuilder();
        description.append("Risk score ").append(r.riskScore).append(" out of 100.");
        for (int i = 0; i < Math.min(2, r.reasons.size()); i++) {
            description.append("\n\n- ").append(r.reasons.get(i));
        }
        description.append("\n\nThis is a risk indicator, not a malware verdict.");

        Map<String, Object> m = new HashMap<>();
        m.put("userId", uid);
        m.put("appName", r.appName);
        m.put("packageName", r.packageName);
        m.put("riskLevel", r.riskLevel.label);
        m.put("riskScore", r.riskScore);
        m.put("description", description.toString());
        m.put("recommendation", r.recommendation);
        m.put("timestamp", new Date());
        m.put("isRead", false);
        m.put("dismissed", false);
        return m;
    }

    /** Loads the user's alerts that were not dismissed, newest first. */
    public void loadAlerts(final Callback<List<SecurityAlert>> callback) {
        String uid = uid();
        if (uid == null) {
            callback.onError("You are not signed in.");
            return;
        }
        db.collection(ALERTS).whereEqualTo("userId", uid).get()
                .addOnSuccessListener(snapshot -> {
                    List<SecurityAlert> list = new ArrayList<>();
                    for (DocumentSnapshot d : snapshot.getDocuments()) {
                        if (Boolean.TRUE.equals(d.getBoolean("dismissed"))) continue;
                        SecurityAlert a = new SecurityAlert();
                        a.id = d.getId();
                        a.appName = stringOf(d.getString("appName"));
                        a.packageName = stringOf(d.getString("packageName"));
                        a.riskLevel = RiskLevel.fromLabel(stringOf(d.getString("riskLevel")));
                        a.riskScore = intOf(d.get("riskScore"));
                        a.description = stringOf(d.getString("description"));
                        a.recommendation = stringOf(d.getString("recommendation"));
                        Date t = d.getDate("timestamp");
                        a.timestamp = t == null ? 0 : t.getTime();
                        a.isRead = Boolean.TRUE.equals(d.getBoolean("isRead"));
                        list.add(a);
                    }
                    Collections.sort(list, (x, y) -> Long.compare(y.timestamp, x.timestamp));
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> callback.onError(message(e)));
    }

    public void markAlertRead(String alertId) {
        db.collection(ALERTS).document(alertId).update("isRead", true);
    }

    public void dismissAlert(String alertId, final Callback<Void> callback) {
        db.collection(ALERTS).document(alertId).update("dismissed", true, "isRead", true)
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(message(e)));
    }

    // ---------------------------------------------------------------- deleting data

    /** Deletes every document of the signed-in user in one collection. Returns how many were deleted. */
    public void deleteAllMine(String collection, final Callback<Integer> callback) {
        String uid = uid();
        if (uid == null) {
            callback.onError("You are not signed in.");
            return;
        }
        db.collection(collection).whereEqualTo("userId", uid).get()
                .addOnSuccessListener(snapshot -> {
                    List<DocumentSnapshot> docs = snapshot.getDocuments();
                    if (docs.isEmpty()) {
                        callback.onSuccess(0);
                        return;
                    }
                    List<Task<Void>> tasks = new ArrayList<>();
                    for (int start = 0; start < docs.size(); start += MAX_BATCH) {
                        WriteBatch batch = db.batch();
                        int end = Math.min(start + MAX_BATCH, docs.size());
                        for (int i = start; i < end; i++) {
                            batch.delete(docs.get(i).getReference());
                        }
                        tasks.add(batch.commit());
                    }
                    final int count = docs.size();
                    Tasks.whenAll(tasks)
                            .addOnSuccessListener(v -> callback.onSuccess(count))
                            .addOnFailureListener(e -> callback.onError(message(e)));
                })
                .addOnFailureListener(e -> callback.onError(message(e)));
    }

    /** Deletes scan history, alerts and device info. The account itself stays. */
    public void deleteAllUserData(final Callback<Integer> callback) {
        String[] collections = {SCANS, ALERTS, DEVICES};
        final AtomicInteger remaining = new AtomicInteger(collections.length);
        final AtomicInteger total = new AtomicInteger(0);
        final AtomicBoolean failed = new AtomicBoolean(false);
        for (String collection : collections) {
            deleteAllMine(collection, new Callback<Integer>() {
                @Override
                public void onSuccess(Integer count) {
                    total.addAndGet(count);
                    if (remaining.decrementAndGet() == 0 && !failed.get()) {
                        callback.onSuccess(total.get());
                    }
                }

                @Override
                public void onError(String message) {
                    if (failed.compareAndSet(false, true)) {
                        callback.onError(message);
                    }
                }
            });
        }
    }

    // ---------------------------------------------------------------- helpers

    private static String message(Exception e) {
        if (e instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException.Code code = ((FirebaseFirestoreException) e).getCode();
            if (code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                return "Permission denied by Firestore rules. Publish firebase/firestore.rules in the Firebase console.";
            }
            if (code == FirebaseFirestoreException.Code.UNAVAILABLE) {
                return "No internet connection.";
            }
        }
        return e.getMessage() == null ? "Unknown error." : e.getMessage();
    }

    private static int intOf(Object value) {
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    private static String stringOf(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
