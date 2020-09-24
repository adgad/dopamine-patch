package com.adgad.dopamine;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ArrayAdapter;

import androidx.preference.PreferenceManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import static java.util.Map.entry;

public class DopeBuilder {


    // variable to hold context
    private Context context;
    private Random generator = new Random();

    private static int ONE_DAY = 86400000;
    private static final String CAMERA_IMAGE_BUCKET_NAME =
            Environment.getExternalStorageDirectory().toString()
                    + "/DCIM/Camera";
    public static final String CAMERA_IMAGE_BUCKET_ID =
            getBucketId(CAMERA_IMAGE_BUCKET_NAME);

    private static final String[] NOTIFICATION_TYPES = new String[]{
        "like", "retweet", "image_like", "tweet_reply", "post_reply", "image_reply"
    };

    String[] instaReplies;
    String[] tweetReplies;

//save the context recievied via constructor in a local variable
    /**
     * Matches code in MediaProvider.computeBucketValues. Should be a common
     * function.
     */
    public static String getBucketId(String path) {
        return String.valueOf(path.toLowerCase().hashCode());
    }
    public DopeBuilder(Context context){
        this.context=context;
        instaReplies = context.getResources().getStringArray(R.array.image_replies);
        tweetReplies = context.getResources().getStringArray(R.array.text_replies);

    }

    public void sendDopamineHit() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int frequency = preferences.getInt("frequency", 20);
        String latestPost = preferences.getString("latestPost", null);

        String[] contact = this.getRandomContact();
        int delay = generator.nextInt(ONE_DAY / frequency);
        Log.d("dopamine", frequency + " " + delay);

        this.scheduleNotification(this.getNotification(contact[0], contact[1], latestPost), delay);
    }

    public void sendDopamineHit(int delay) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String latestPost = preferences.getString("latestPost", null);
        String[] contact = this.getRandomContact();
        this.scheduleNotification(this.getNotification(contact[0], contact[1], latestPost), delay);
    }

    private Bitmap getRecentImage() {
            final String[] projection = { MediaStore.Images.Media.DATA };
            final String selection = MediaStore.Images.Media.BUCKET_ID + " = ?";
            final String[] selectionArgs = { CAMERA_IMAGE_BUCKET_ID };
            final Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    null);
            ArrayList<String> result = new ArrayList<String>(cursor.getCount());
            if (cursor.moveToFirst()) {
                final int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                do {
                    final String data = cursor.getString(dataColumn);
                    result.add(data);
                } while (cursor.moveToNext());
            }
            cursor.close();

            if(result.size() > 0) {
                String fileName = result.get(this.generator.nextInt(result.size()));
                File imgFile = new File(fileName);
                if(imgFile.exists()){
                    BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                    bmOptions.inSampleSize = 2;
                    bmOptions.inJustDecodeBounds = false;
                    Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath(),bmOptions);
                    return bitmap;
                }
            }
            return null;
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
        notificationIntent.putExtra(NotificationPublisher.NOTIFICATION_ID, this.generator.nextInt(100));
        notificationIntent.putExtra(NotificationPublisher.NOTIFICATION, notification);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this.context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        long futureInMillis = SystemClock.elapsedRealtime() + delay;
        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, futureInMillis, pendingIntent);

    }


    private Notification getNotification(String name, String userImage, String latestPost) {

        String type = NOTIFICATION_TYPES[this.generator.nextInt(NOTIFICATION_TYPES.length)];
        Notification.Builder builder = new Notification.Builder(this.context);
        String text;
        String actor = name;
        int others = this.generator.nextInt(13) - 7;
        if(others > 0) {
            actor += " and " + others + " others";
        }
        switch(type) {
            case "like":
                builder.setContentTitle(actor);
                builder.setSmallIcon(R.drawable.ic_favorite);

                if(latestPost != null && !latestPost.isEmpty()) {
                    builder.setStyle(new Notification.BigTextStyle()
                            .bigText("Liked: " + latestPost));
                } else {
                    builder.setContentText("liked your tweet");
                }
                break;
            case "retweet":
                builder.setContentTitle(actor);
                builder.setSmallIcon(R.drawable.ic_favorite);

                if(latestPost != null && !latestPost.isEmpty()) {
                    builder.setStyle(new Notification.BigTextStyle()
                            .bigText("RT: " + latestPost));
                }
                builder.setContentText("retweeted your tweet");
                break;
            case "image_like":
                builder.setContentTitle(actor);
                builder.setContentText("liked your photo");
                builder.setSmallIcon(R.drawable.ic_favorite);
                break;
            case "tweet_reply":
                builder.setContentTitle(name);
                builder.setSmallIcon(R.drawable.comment);

                text =  "replied to your tweet: "  + tweetReplies[this.generator.nextInt(tweetReplies.length)];
                builder.setStyle(new Notification.BigTextStyle()
                        .bigText(text));
                builder.setContentText(text);
                break;
            case "post_reply":
                builder.setContentTitle(actor);
                builder.setSmallIcon(R.drawable.comment);

                text = "commented: "  + tweetReplies[this.generator.nextInt(tweetReplies.length)];
                builder.setStyle(new Notification.BigTextStyle()
                        .bigText(text));
                builder.setContentText(text);
                break;
            case "image_reply":

                builder.setContentTitle(name);
                builder.setSmallIcon(R.drawable.comment);
                text = "commented: "  + instaReplies[this.generator.nextInt(instaReplies.length)];
                builder.setStyle(new Notification.BigTextStyle()
                        .bigText(text));
                builder.setContentText(text);
                break;
        }

        if(userImage != null) {
            Uri imageUri = Uri.parse(userImage);
            Bitmap icon = null;
            try {
                icon = MediaStore.Images.Media.getBitmap(this.context.getContentResolver(), imageUri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            builder.setLargeIcon(icon);
        }

        if(type.contains("image")) {
            Bitmap recentImage = getRecentImage();

            builder.setStyle(new Notification.BigPictureStyle()
                    .bigPicture(recentImage));


        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId("dopamine_hit");
        }

        return builder.build();
    }
}
