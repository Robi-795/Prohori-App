# Prohori Android: setup guide (Banglish)

Ei project Android Studio te khulte hobe. Ami APK baniye dite pari ni, karon ekhane Android SDK ar internet nei. Tai project source code ta zip e dilam, tumi Android Studio te Run korle app phone e install hobe.

## Ki ki lagbe
- Android Studio (notun version) ar internet (prothombar Gradle onek file download kore)
- Ekta Google account (Firebase er jonno)
- Ekta Android phone (USB debugging on) ba emulator. Real phone bhalo, karon emulator e onek kom app thake, scan e kichu dekhabe na.

## Step 1: Project khola
1. Android Studio te **File > Open** e jao, `ProhoriAndroid` folder ta select koro (zip extract kore nio).
2. "Trust project" e click koro, Gradle sync er jonno opekkha koro (prothombar 5-10 minute lagte pare).
3. Sync e error ashbe: `app/google-services.json is missing`. Eta normal. Step 2 korle jabe.
4. Jodi "SDK 35 not installed" jatiyo kichu ashe, error er pashe "Install missing SDK package" link e click koro.

## Step 2: Firebase project baniye google-services.json nao
1. https://console.firebase.google.com e jao > **Add project** > nam dao `Prohori` (Google Analytics off rakhte paro).
2. Project overview e Android icon e click koro. **Package name** hubohu eta likho: `com.prohori.app`. App nickname optional. **Register app** click koro.
3. `google-services.json` download koro ar `ProhoriAndroid/app/` folder e rakho (`app` folder er bhitore, `src` folder er bairey).
4. Firebase er baki "Add SDK" step gulo skip koro, oigulo project e kora ache.
5. Android Studio te **File > Sync Project with Gradle Files** koro.

## Step 3: Login chalu koro
Firebase console > **Build > Authentication > Get started > Sign-in method > Email/Password > Enable > Save**.

## Step 4: Database ar security rules
1. **Build > Firestore Database > Create database**. Location nao nikoter ta (jemon `asia-south1`). Mode: **Production**.
2. **Rules** tab e jao. Ager shob muche `firebase/firestore.rules` file er shob content paste koro. **Publish** click koro.

Rules publish na korle app e `Permission denied by Firestore rules` error ashbe.

## Step 5: Run
1. Phone e Settings > About phone > Build number e 7 bar tap kore Developer options on koro, tarpor **USB debugging** on koro.
2. USB cable diye phone connect koro. Android Studio er upore device select kore **Run (green play button)** click koro.
3. App khulle: **Register** koro > "Save your scans in the cloud?" dialog e **Allow** > notification permission dao.
4. **Scan** tab e jao > **Start Quick Scan**.

## Step 6: Push notification test (optional)
1. Cloud sync **Allow** thakte hobe (tahole device info Firestore e save hoy).
2. Firebase console > Firestore > `device_information` > tomar document > `fcmToken` value copy koro.
3. Console > **Engage > Messaging > Create your first campaign > Firebase Notification messages > Send test message** e token paste kore Test koro.

Push pathano app theke hoy na (server credential app e rakha nirapod na). Real use e Cloud Functions ba backend lagbe.

## Kono problem hole
| Problem | Karon / Solution |
|---|---|
| `google-services.json is missing` | Step 2 er 3 number step. File `app/` folder e ache ki dekho |
| Package name mismatch error | Firebase e package name `com.prohori.app` hubohu hote hobe |
| Login e "Something went wrong" | Step 3 e Email/Password enable korecho ki dekho |
| `Permission denied by Firestore rules` | Step 4 er rules Publish koro |
| Scan e onek kom app dekhay | Emulator e kom app thake. Real phone e try koro |
| Gradle sync fail | Internet check koro. Android Studio te **File > Invalidate Caches > Restart** |

## Ami ki test korechi ar ki kori ni
- Risk engine er 14 ta unit test ei sandbox e chalate perechi, shob pass hoyeche.
- Android code (screen, Firebase, scanner) Android Studio diye compile ba phone e run kora hoy ni. XML valid, resource reference gulo match kore ar Java syntax thik, eta check korechi.
- Tai prothom Run e choto kono error ashte pare. Error message ta amake pathaile thik kore dibo.
