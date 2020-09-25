package com.adgad.dopamine;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.legacy.content.WakefulBroadcastReceiver;

public class BootReceiver extends BroadcastReceiver {

    public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
                DopeBuilder dopeBuilder = new DopeBuilder(context);
                dopeBuilder.sendDopamineHit();
            }
        }


}