package com.example.notificationalerter;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.HashSet;
import java.util.Set;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class MyNotificationListenerService extends NotificationListenerService {
    private static final String TAG = "NotificationListener";
    private static final String ACTION_NOTIFICATION_POSTED = "com.example.notificationalerter.NOTIFICATION_POSTED";
    private static final String EXTRA_SOUND_URI = "sound_uri";

    private static Set<String> selectedApps = new HashSet<>();
    private static String searchWord;
    private static Uri customSoundUri;

    private static final String CHANNEL_ID = "alerter";
    private static final int NOTIFICATION_ID = 175;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "NotificationListenerService onCreate");
        createNotificationChannel();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "NotificationListenerService onDestroy");
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        Log.d(TAG, "NotificationListenerService onListenerConnected");

        // Service is connected, start listening to notifications
        startListening();
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        Log.d(TAG, "NotificationListenerService onListenerDisconnected");

        // Service is disconnected, stop listening to notifications
        stopListening();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        Log.d(TAG, "Notification posted: " + sbn.getPackageName());

        String packageName = sbn.getPackageName();
        if (isAppSelected(packageName)) {
            String title = sbn.getNotification().extras.getString(Notification.EXTRA_TITLE);
            String text = sbn.getNotification().extras.getString(Notification.EXTRA_TEXT);
            Log.d(TAG, "Title: " + title);
            Log.d(TAG, "Text: " + text);

            if (containsSearchWord(text)) {
                if (customSoundUri != null) {
                    playCustomNotificationSound(customSoundUri);
                } else {
                    playDefaultNotificationSound();
                }

                sendNotificationBroadcast(packageName, title, text);
            }
        }
    }

    private boolean containsSearchWord(String text) {
        if (searchWord == null || searchWord.isEmpty()) {
            return false;
        }

        String[] words = text.toLowerCase().split("\\W+");
        String lowercaseSearchWord = searchWord.toLowerCase();
        for (String word : words) {
            if (word.equals(lowercaseSearchWord)) {
                return true;
            }
        }

        return false;
    }


    private boolean isAppSelected(String packageName) {
        return selectedApps.contains(packageName);
    }

    private boolean containsSearchWord(StatusBarNotification sbn) {
        if (searchWord == null || searchWord.isEmpty()) {
            return true;
        }

        String title = sbn.getNotification().extras.getString(Notification.EXTRA_TITLE);
        String text = sbn.getNotification().extras.getString(Notification.EXTRA_TEXT);

        return (title != null && title.toLowerCase().contains(searchWord.toLowerCase()))
                || (text != null && text.toLowerCase().contains(searchWord.toLowerCase()));
    }

    private void playDefaultNotificationSound() {
        try {
            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            playNotificationSound(defaultSoundUri);
        } catch (Exception e) {
            Log.e(TAG, "Error playing default notification sound: " + e.getMessage());
        }
    }

    private void playCustomNotificationSound(Uri soundUri) {
        if (soundUri == null) {
            return;
        }

        try {
            playNotificationSound(soundUri);
        } catch (Exception e) {
            Log.e(TAG, "Error playing custom notification sound: " + e.getMessage());
        }
    }

    private void playNotificationSound(Uri soundUri) {
        try {
            Context context = getApplicationContext();
            Ringtone ringtone = RingtoneManager.getRingtone(context, soundUri);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ringtone.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build());
            } else {
                ringtone.setStreamType(AudioManager.STREAM_NOTIFICATION);
            }

            ringtone.play();
        } catch (Exception e) {
            Log.e(TAG, "Error playing notification sound: " + e.getMessage());
        }
    }

    private void sendNotificationBroadcast(String packageName, String title, String text) {
        // Create an explicit intent for MainActivity
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // Create a PendingIntent for the notification
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_IMMUTABLE);

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_low_battery)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // Get the notification manager
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());

        // Post the notification
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    public static void addSelectedApp(String packageName) {
        selectedApps.add(packageName);
    }

    public static void removeSelectedApp(String packageName) {
        selectedApps.remove(packageName);
    }

    public static void setSearchWord(String word) {
        searchWord = word;
    }

    public static Uri getCustomSoundUri() {
        return customSoundUri;
    }

    public static void setCustomSoundUri(Uri uri) {
        customSoundUri = uri;
    }

    private void startListening() {
        setSearchWord(searchWord);
    }

    private void stopListening() {
        setSearchWord(null);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Alerter Channel";
            String description = "Channel for alerter notifications";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
