package com.adgad.dopamine;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.util.Log;

import androidx.preference.PreferenceManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

public class DopeBuilder {


    // variable to hold context
    private Context context;
    private Random generator = new Random();

    private static int ONE_DAY = 86400000;

//save the context recievied via constructor in a local variable

    public DopeBuilder(Context context){
        this.context=context;
    }

    public void sendDopamineHit() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int frequency = preferences.getInt("frequency", 20);

        String[] contact = this.getRandomContact();
        int delay = generator.nextInt(ONE_DAY / frequency);
        Log.d("dopamine", frequency + " " + delay);

        this.scheduleNotification(this.getNotification(contact[0], contact[1]), delay);
    }

    private String[] getRandomContact() {
        Cursor managedCursor = context.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,null, null, null, null);
        int size = managedCursor.getCount();

        boolean found = false;
        String[] results = new String[2];
        Random rnd = new Random();
        int index = rnd.nextInt(size);
        managedCursor.moveToPosition(index);
        results[0] = managedCursor.getString(managedCursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
        results[1] = managedCursor.getString(managedCursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI));
        return results;
    }

    private void scheduleNotification(Notification notification, int delay) {

        Intent notificationIntent = new Intent(this.context, NotificationPublisher.class);
        notificationIntent.putExtra(NotificationPublisher.NOTIFICATION_ID, 1);
        notificationIntent.putExtra(NotificationPublisher.NOTIFICATION, notification);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this.context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        long futureInMillis = SystemClock.elapsedRealtime() + delay;
        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, futureInMillis, pendingIntent);

    }

    private String getRandomTitle() {
        String[] apps = new String[]{"Instagram", "Twitter", "Facebook"};
        return apps[this.generator.nextInt(apps.length)];
    }

    private String getRandomText() {
        String[] text = new String[]{"liked your tweet", "replied to your tweet", "liked your photo", "liked your story"};
        return text[this.generator.nextInt(text.length)];
    }

    private Notification getNotification(String name, String image) {
        Notification.Builder builder = new Notification.Builder(this.context);
        builder.setContentTitle(getRandomTitle());
        builder.setContentText(name + " " + getRandomText());
        InputStream inputStream = null;
        try {
            if(image != null) {
                Uri imageUri = Uri.parse(image);
                Bitmap icon = MediaStore.Images.Media.getBitmap(this.context.getContentResolver(), imageUri);
                builder.setLargeIcon(icon);
            }
            builder.setSmallIcon(R.drawable.ic_launcher_foreground);

        } catch (IOException e) {
            builder.setSmallIcon(R.drawable.ic_launcher_foreground);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId("dopamine_hit");
        }

        return builder.build();
    }
}
