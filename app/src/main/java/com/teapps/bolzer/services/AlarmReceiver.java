package com.teapps.bolzer.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.text.Html;
import android.util.Log;

import com.teapps.bolzer.R;


public class AlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "CHANNEL-BOLZER-ID";

    @Override
    public void onReceive(Context context, Intent intent) {
        createNotificationChannel(context);
        String id_string = intent.getExtras().getString(context.getString(R.string.KEY_ID));
        String title_string = intent.getExtras().getString(context.getString(R.string.KEY_TITLE));
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean settings_notifications = sharedPreferences.getBoolean("notifications", true);
        if (settings_notifications) {
            showNotification(id_string, title_string, context);
        } else {
            Log.d("SETTINGS", "Keine Benachrichtigungen zugelassen");
        }

    }

    private void showNotification(String id_string, String title_string, Context context) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle("Bolzer Erinnerung")
                .setContentText(Html.fromHtml("Der Bolzer " + "<b>" + title_string
                        + "</b>" + " beginnt in 30 Minuten!"))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(Html.fromHtml("Der Bolzer " + "<b>" + title_string
                        + "</b>" + " beginnt in 30 Minuten!")))
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(1357, builder.build());
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "channel_bolzer";
            String description = "channel bolzer description";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel( CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
