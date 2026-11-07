package com.prohori.app.security;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Knows which installer packages are recognised app stores. */
public final class TrustedSources {

    private static final Set<String> TRUSTED = new HashSet<>(Arrays.asList(
            "com.android.vending",            // Google Play Store
            "com.sec.android.app.samsungapps", // Samsung Galaxy Store
            "com.amazon.venezia",             // Amazon Appstore
            "com.huawei.appmarket",           // Huawei AppGallery
            "com.xiaomi.mipicks",             // Xiaomi GetApps
            "com.heytap.market",              // OPPO / realme
            "com.vivo.appstore"               // vivo
    ));

    private TrustedSources() { }

    public static boolean isTrusted(String installer) {
        return installer != null && TRUSTED.contains(installer);
    }

    /** Human readable install source for the UI. */
    public static String label(String installer, boolean systemApp) {
        if (systemApp) return "Pre-installed (system app)";
        if (installer == null) return "Unknown source (installer not reported)";
        if ("com.android.vending".equals(installer)) return "Google Play Store";
        if ("com.sec.android.app.samsungapps".equals(installer)) return "Samsung Galaxy Store";
        if ("com.amazon.venezia".equals(installer)) return "Amazon Appstore";
        if ("com.huawei.appmarket".equals(installer)) return "Huawei AppGallery";
        if ("com.xiaomi.mipicks".equals(installer)) return "Xiaomi GetApps";
        if ("com.heytap.market".equals(installer)) return "OPPO / realme App Market";
        if ("com.vivo.appstore".equals(installer)) return "vivo App Store";
        if (installer.contains("packageinstaller")) return "Installed from an APK file";
        return "Installed by " + installer;
    }
}
