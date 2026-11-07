# Prohori Android: test cases

Status rules: **Passed** only if the test was actually executed. Everything else is **Not executed**.
Fill in "Actual result" and "Status" when you run each case on a device.

What was executed so far: the 14 unit tests in `RiskAnalyzerTest` were run on a JVM in the build sandbox with a minimal stand-in for JUnit (not inside Android Studio). The Android screens, Firebase calls and scanner were not run on a phone.

| ID | Objective | Preconditions | Input | Expected result | Actual result | Status |
|---|---|---|---|---|---|---|
| TC-01 | User registration | Firebase configured, Email/Password enabled, internet on | Name `Test User`, email `test@example.com`, password `Prohori123`, same confirm | Account created, dashboard opens, `users/{uid}` has name and email, no password stored | | Not executed |
| TC-02 | User login | Account from TC-01 exists | Correct email and password | Dashboard opens, welcome shows the name | | Not executed |
| TC-03 | Incorrect password | Account exists | Correct email, wrong password | Message "Incorrect email or password.", stays on login screen. Same message for an unknown email | | Not executed |
| TC-04 | Password reset | Account exists, internet on | Registered email, then an unregistered email | Same neutral message both times. Reset email arrives for the registered one | | Not executed |
| TC-05 | Dashboard data display | Signed in | Open Home before and after a Quick Scan | Before: "No scan yet" and zeros. After: totals and risk counts equal the scan result screen | | Not executed |
| TC-06 | Quick Scan | Real phone with installed apps | Scan tab > Start Quick Scan | Progress shows, result screen opens. Total equals the number of user-installed apps in Installed apps list | | Not executed |
| TC-07 | Installed app inspection | Scan done | Tap an app > Open Android app info | Details show version, source, reasons, permissions, action. Button opens the system App Info page | | Not executed |
| TC-08 | Permission risk analysis | JVM | `RiskAnalyzerTest`: no permissions, internet only, messaging app from Play, sideloaded SMS app, granted vs not granted, trusted store and system app reduction | Scores follow the documented rules | 14 of 14 unit tests passed in sandbox (stand-in JUnit runner, not Android Studio) | Passed (unit level) |
| TC-09 | Risk classification | JVM | `RiskAnalyzerTest`: score thresholds 11/12/34/35/59/60/84/85/100, accessibility and keyboard scenarios, banking trojan pattern | Levels Safe, Low, Medium, High, Critical match thresholds. Score stays within 0-100. Every result has a reason and recommendation | 14 of 14 unit tests passed in sandbox (same run) | Passed (unit level) |
| TC-10 | Security alert generation | Cloud sync on. A test app that is sideloaded with accessibility switched on | Run Quick Scan twice, then dismiss the alert, scan again | Alert appears after first scan. No duplicate after second. Dismissed alert stays dismissed | | Not executed |
| TC-11 | FCM notifications | Cloud sync on, token in `device_information` | Firebase console test message to the token, app in foreground then background | Notification shows, tapping it opens the Alerts tab | | Not executed |
| TC-12 | Scan history saving | Cloud sync on | Run a scan, open History. Then turn cloud sync off and scan again | First scan saved and listed. With sync off nothing new is saved | | Not executed |
| TC-13 | Firestore access control | Two accounts A and B, scan saved for A | Read A's documents as B in the Rules playground or a test build | Permission denied. A can read own data | | Not executed |
| TC-14 | Logout | Signed in | Profile > Log out > confirm, then press Back | Login screen shows. Back does not return to the dashboard. Local settings cleared | | Not executed |
| TC-15 | Network failure handling | Signed in | Airplane mode: try login, run a scan, open History | Login shows "No internet connection". Scan still works locally. History shows an error text. No crash | | Not executed |
