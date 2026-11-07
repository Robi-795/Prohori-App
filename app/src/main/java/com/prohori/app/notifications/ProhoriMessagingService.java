package com.prohori.app.notifications;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.prohori.app.firebase.FirebaseManager;
import com.prohori.app.utils.LocalStore;

/**
 * Receives Firebase Cloud Messaging events.
 * - onNewToken: the push token changed, so update it in the cloud (only if the user allowed cloud sync).
 * - onMessageReceived: a push arrived while the app is open, show it as a notification.
 * When the app is in the background, Android shows push notifications by itself.
 *
 * Pushes are sent from a trusted place (Firebase console or Cloud Functions), never from this app.
 */
public class ProhoriMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        LocalStore store = new LocalStore(this);
        if (FirebaseManager.get().uid() != null && store.isCloudSyncEnabled()) {
            FirebaseManager.get().saveDeviceInfo(store.deviceId(), token);
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);
        String title = "Prohori security alert";
        String body = "";
        if (message.getNotification() != null) {
            if (message.getNotification().getTitle() != null) title = message.getNotification().getTitle();
            if (message.getNotification().getBody() != null) body = message.getNotification().getBody();
        } else {
            if (message.getData().get("title") != null) title = message.getData().get("title");
            if (message.getData().get("body") != null) body = message.getData().get("body");
        }
        if (new LocalStore(this).isNotificationsEnabled()) {
            NotificationHelper.show(this, title, body);
        }
    }
}
