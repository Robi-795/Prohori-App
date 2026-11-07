# PROHORI Android

Android companion of the Prohori intrusion detection project. It inspects the apps installed on a phone, rates them with a transparent rule-based risk engine, and keeps scan history and alerts in the user's own private Firebase account.

Java, XML layouts, Firebase Authentication, Cloud Firestore, Firebase Cloud Messaging. Minimum Android 6.0 (API 23).

**Start here:** `SETUP_GUIDE.md` (Banglish, step by step).

## What the app does

| Area | Details |
|---|---|
| Login | Register, sign in, forgot password, logout (Firebase Email/Password). Wrong email and wrong password give the same message. |
| Dashboard | Last scan time, apps inspected, apps to review, Critical/High/Medium/Low counts, recent alerts, shortcuts. All numbers come from real scan data. |
| Quick Scan | Apps you installed: permissions, install source, and which accessibility service, keyboard, notification listener or device admin is switched on. |
| Deep Inspection | Quick Scan plus system apps and the special services each app declares in its manifest. |
| Installed apps | Searchable list of all inspected apps with risk badges. |
| App details | Version, install source, rating with reasons, permission-by-permission explanations, recommended action, button to open Android App Info. |
| Alerts | Critical and High findings, read/unread, details, dismiss. No duplicates on repeated scans. |
| History | Saved scans, details, delete one (long-press) or all, with confirmation. |
| Settings | Notifications, include system apps, cloud sync, privacy text, terms, delete my cloud data. |

## Risk engine (all rules are visible)

Score = sum of rules, then clamped to 0-100.

| Rule | Points |
|---|---|
| Sensitive permission (table in `security/PermissionRules.java`) | 2 to 20 each. A runtime permission that is requested but not granted counts half |
| Accessibility service switched ON | 40 (declared but off: 8) |
| Keyboard (input method) enabled | 25 (declared but off: 3) |
| Notification access ON | 25 (declared but off: 4) |
| Active device admin | 25 (declared but off: 6) |
| Combinations | SMS + internet 12, accessibility ON + internet 20, keyboard ON + internet 10, overlay + accessibility 15, mic + camera + internet 6, contacts + SMS + call log 10, background location + internet 6, sideloaded + install-apps 10, sideloaded + device admin 15, sideloaded keyboard 20 |
| App information | Installed outside a store 10, debuggable 8, targets API below 23: 6 |
| Adjustments | From a recognised store: score x 0.6. System app: score x 0.3 |

| Score | Level |
|---|---|
| 85-100 | Critical |
| 60-84 | High |
| 35-59 | Medium |
| 12-34 | Low |
| 0-11 | Safe |

"Apps to review" means Medium, High and Critical. A high score means "worth a closer look", never "this app is malware". The UI repeats this.

## Project layout

```
app/src/main/java/com/prohori/app/
  activities/     Splash, Login, Register, ForgotPassword, Main (5 tabs), ScanResult,
                  InstalledApps, AppDetails, AlertDetails, Settings, About
  fragments/      Home (dashboard), Scan, Alerts, History, Profile
  adapters/       AppsAdapter, AlertsAdapter, HistoryAdapter
  models/         RiskLevel, RiskResult, ScanReport, SecurityAlert, AppSnapshot
  security/       RiskAnalyzer, PermissionRules, TrustedSources   (pure Java, unit tested)
  scanner/        SecurityScanner, EnabledServices                (PackageManager and system APIs)
  firebase/       FirebaseManager, AuthErrors
  notifications/  NotificationHelper, ProhoriMessagingService
  utils/          LocalStore (SharedPreferences), UiUtils
app/src/test/     RiskAnalyzerTest (14 tests, run on the JVM)
firebase/         firestore.rules
```

## Firestore data

- `users/{uid}`: name, email, createdAt
- `scan_history/{id}`: userId, scanType, scanDate, totalAppsScanned, riskyApps, riskSummary, findings (Medium and above, max 100)
- `security_alerts/{uid_package}`: userId, appName, packageName, riskLevel, riskScore, description, recommendation, timestamp, isRead, dismissed
- `device_information/{uid_deviceId}`: userId, deviceName, androidVersion, lastSync, fcmToken

Rules (`firebase/firestore.rules`) let a user touch only documents where `userId` equals their uid. Queries filter by `userId` only and sort on the phone, so no composite index is required.

## Privacy and security choices

- Nothing is uploaded until the user says yes (first-launch dialog, switchable in Settings). With cloud sync off, scans still work and stay on the phone.
- No keyboard input, messages, files or passwords are read. Passwords are handled only by Firebase Authentication.
- The app never uninstalls, disables or changes other apps.
- No background monitoring. Scans run only when the user starts them.
- `allowBackup` is off. No server credentials are inside the app.

## Limits you should know

- **Android sandbox.** A normal app cannot read keystrokes, look inside other apps or list all running processes. Prohori uses permissions, install source and special services that the user enabled. It is a risk review, not an antivirus.
- **QUERY_ALL_PACKAGES.** Needed to list all apps on Android 11+. Fine for a class project or sideloading, but Google Play restricts this permission and needs a justification.
- **Install source.** Some devices do not report which store installed an app. Those apps show "Unknown source" and get the sideloaded points.
- **Push notifications.** The app registers the token and shows incoming pushes. Sending needs the Firebase console or a trusted backend (for example Cloud Functions). Server keys must never be put in the app.
- **Django backend not connected.** The existing backend has no authentication (every endpoint is open and `@csrf_exempt`, `/clear/` deletes all alerts for anyone) and its data model is Windows-process shaped (`pid`, `process_name`). Connecting Android safely needs new Android endpoints that verify the Firebase ID token on the server first. This project works fully without it. Also before deployment: `DEBUG = True`, `ALLOWED_HOSTS = ["*"]` and a hard-coded `SECRET_KEY` in `settings.py` should be changed.

## Build facts

AGP 8.7.3, Gradle 8.9, compileSdk 35, targetSdk 34, minSdk 23, Material 1.12.0, Firebase BoM 33.7.0. Open in Android Studio (the Gradle wrapper jar is not included; Android Studio downloads Gradle from `gradle-wrapper.properties`).
