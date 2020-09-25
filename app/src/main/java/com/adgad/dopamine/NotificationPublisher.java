package com.adgad.dopamine;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.legacy.content.WakefulBroadcastReceiver;

import java.util.Random;

public class NotificationPublisher extends BroadcastReceiver {

    public static String NOTIFICATION_ID = "notification-id";
    public static String NOTIFICATION_TYPE = "notification-type";
    public static String NOTIFICATION_NAME = "notification-name";
    public static String NOTIFICATION_LATEST_POST = "notification-latestPost";
    public static String NOTIFICATION_USER_IMAGE = "notification-userImage";
    public static String NOTIFICATION_RECENT_IMAGE_FILE = "notification-recentImageFile";
    public static String NOTIFICATION_RECENT_IMAGE_LABEL = "notification-recentImageLabel";

    public static Random random = new Random();

    public void onReceive(Context context, Intent intent) {

        DopeBuilder dopeBuilder = new DopeBuilder(context);
        NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

// === Removed some obsoletes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            String channelId = "dopamine_hit";
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }
        Notification notification = null;
        if(intent.getStringExtra(NOTIFICATION_TYPE).contains("image")) {
            notification = dopeBuilder.getImageNotification(
                    intent.getStringExtra(NOTIFICATION_TYPE),
                    intent.getStringExtra(NOTIFICATION_NAME),
                    intent.getStringExtra(NOTIFICATION_USER_IMAGE),
                    intent.getStringExtra(NOTIFICATION_RECENT_IMAGE_FILE),
                    intent.getStringExtra(NOTIFICATION_RECENT_IMAGE_LABEL)
            );

        } else {
            notification = dopeBuilder.getNotification(
                    intent.getStringExtra(NOTIFICATION_TYPE),
                    intent.getStringExtra(NOTIFICATION_NAME),
                    intent.getStringExtra(NOTIFICATION_USER_IMAGE),
                    intent.getStringExtra(NOTIFICATION_LATEST_POST)
            );
        }

        int id = intent.getIntExtra(NOTIFICATION_ID, 0);
        notificationManager.notify(id, notification);
        dopeBuilder.sendDopamineHit();

    }
}