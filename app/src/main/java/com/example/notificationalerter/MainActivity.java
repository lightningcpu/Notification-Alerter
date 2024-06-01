package com.example.notificationalerter;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_NOTIFICATION_ACCESS = 1;
    private static final int REQUEST_SOUND_PICKER = 2;
    private static final String NOTIFICATION_CHANNEL_ID = "alerter_channel_id";

    private Button toggleListenerButton;
    private ListView appListView;
    private AppAdapter appAdapter;
    private boolean isListenerEnabled;
    private EditText searchInput;

    private static final String PREF_SELECTED_SOUND_URI = "selected_sound_uri";
    private Button chooseNotificationButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toggleListenerButton = findViewById(R.id.toggle_listener_button);
        appListView = findViewById(R.id.app_list_view);
        searchInput = findViewById(R.id.search_input);
        chooseNotificationButton = findViewById(R.id.choose_notification_button);

        isListenerEnabled = false;

        toggleListenerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleListener();
            }
        });

        chooseNotificationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openNotificationSoundPicker();
            }
        });

        appAdapter = new AppAdapter(this, getInstalledApps());
        appAdapter.setOnCheckedChangeListener(new AppAdapter.OnCheckedChangeListener() {
            @Override
            public void onCheckedChange(String packageName, boolean isChecked) {
                if (isChecked) {
                    MyNotificationListenerService.addSelectedApp(packageName);
                    Toast.makeText(MainActivity.this, "Selected: " + packageName, Toast.LENGTH_SHORT).show();
                } else {
                    MyNotificationListenerService.removeSelectedApp(packageName);
                    Toast.makeText(MainActivity.this, "Deselected: " + packageName, Toast.LENGTH_SHORT).show();
                }
            }
        });
        appListView.setAdapter(appAdapter);

        if (isNotificationAccessGranted()) {
            toggleListenerButton.setVisibility(View.VISIBLE);
        } else {
            toggleListenerButton.setVisibility(View.GONE);
            requestNotificationAccessPermission();
        }

        // Retrieve the selected sound URI from SharedPreferences
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String selectedSoundUriString = preferences.getString(PREF_SELECTED_SOUND_URI, null);
        if (selectedSoundUriString != null) {
            Uri selectedSoundUri = Uri.parse(selectedSoundUriString);
            MyNotificationListenerService.setCustomSoundUri(selectedSoundUri);
        }

        createNotificationChannel();
    }

    private List<AppInfo> getInstalledApps() {
        List<AppInfo> appList = new ArrayList<>();
        PackageManager packageManager = getPackageManager();
        List<ApplicationInfo> applications = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
        String appPackageName = getPackageName();

        for (ApplicationInfo applicationInfo : applications) {
            String packageName = applicationInfo.packageName;

            // Exclude the app itself
            if (!packageName.equals(appPackageName) && isLaunchable(packageManager, packageName)) {
                AppInfo appInfo = new AppInfo();
                appInfo.setPackageName(packageName);
                appInfo.setName(applicationInfo.loadLabel(packageManager).toString());
                appInfo.setIcon(applicationInfo.loadIcon(packageManager));
                appList.add(appInfo);
            }
        }

        return appList;
    }

    private boolean isLaunchable(PackageManager packageManager, String packageName) {
        Intent launchIntent = packageManager.getLaunchIntentForPackage(packageName);
        return launchIntent != null;
    }

    private void requestNotificationAccessPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            startActivityForResult(intent, REQUEST_NOTIFICATION_ACCESS);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_NOTIFICATION_ACCESS) {
            if (isNotificationAccessGranted()) {
                toggleListenerButton.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(this, "Notification access not granted.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_SOUND_PICKER) {
            if (resultCode == RESULT_OK) {
                Uri selectedSoundUri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                if (selectedSoundUri != null) {
                    // Save the selected sound URI to SharedPreferences
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString(PREF_SELECTED_SOUND_URI, selectedSoundUri.toString());
                    editor.apply();

                    // Handle the selected sound URI here
                    // You can save it or use it to set the notification sound for your app
                    MyNotificationListenerService.setCustomSoundUri(selectedSoundUri);
                    Toast.makeText(this, "Selected sound: " + selectedSoundUri.toString(), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "No sound selected.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean isNotificationAccessGranted() {
        ComponentName cn = new ComponentName(this, MyNotificationListenerService.class);
        String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return flat != null && flat.contains(cn.flattenToString());
    }

    private void toggleListener() {
        if (isListenerEnabled) {
            stopListener();
        } else {
            startListener();
        }
    }

    private void startListener() {
        toggleListenerButton.setText("Stop Listener");
        isListenerEnabled = true;
        Toast.makeText(this, "Listener started", Toast.LENGTH_SHORT).show();

        // Get the search word from the input field
        String searchWord = searchInput.getText().toString().trim();

        // Pass the search word to the notification listener service
        MyNotificationListenerService.setSearchWord(searchWord);

        // Start the notification listener service
        startService(new Intent(this, MyNotificationListenerService.class));
    }

    private void stopListener() {
        toggleListenerButton.setText("Start Listener");
        isListenerEnabled = false;
        Toast.makeText(this, "Listener stopped", Toast.LENGTH_SHORT).show();

        // Stop the notification listener service
        stopService(new Intent(this, MyNotificationListenerService.class));
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Alerter Channel";
            String description= "Channel for alerter notifications";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void openNotificationSoundPicker() {
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Notification Sound");
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) null);
        startActivityForResult(intent, REQUEST_SOUND_PICKER);
    }
}


