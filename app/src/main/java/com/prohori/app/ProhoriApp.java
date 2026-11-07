package com.prohori.app;

import android.app.Application;

import com.prohori.app.notifications.NotificationHelper;

public class ProhoriApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        NotificationHelper.createChannel(this);
    }
}
