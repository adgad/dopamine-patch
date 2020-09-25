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
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static java.util.Map.entry;

public class DopeBuilder {


    // variable to hold context
    private Context context;
    private Random generator = new Random();

    private ImageLabelerOptions options =
     new ImageLabelerOptions.Builder()
         .setConfidenceThreshold(0.7f)
         .build();
    private ImageLabeler labeler = ImageLabeling.getClient(options);

    private static int ONE_DAY = 86400000;
    private static final String CAMERA_IMAGE_BUCKET_NAME =
            Environment.getExternalStorageDirectory().toString()
                    + "/DCIM/Camera";
    public static final String CAMERA_IMAGE_BUCKET_ID =
            getBucketId(CAMERA_IMAGE_BUCKET_NAME);

    //Added image_reply multiple times so its more likely to appear
    private static final String[] NOTIFICATION_TYPES = new String[]{
        "like", "retweet", "image_like", "tweet_reply", "post_reply", "image_reply", "image_reply", "image_reply"
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
        int delay = generator.nextInt(ONE_DAY / frequency);
        sendDopamineHit(delay);
    }

    public void sendDopamineHit(final int delay) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        final String type = NOTIFICATION_TYPES[this.generator.nextInt(NOTIFICATION_TYPES.length)];
        final String latestPost = preferences.getString("latestPost", null);
        final String[] contact = this.getRandomContact();

        if(type.contains("image")) {

            final File imageFile = getRandomFileFromGallery();
            if(imageFile != null && imageFile.exists()) {
                final Bitmap image = getBitmapFromFile(imageFile);
                OnSuccessListener onSuccess = new OnSuccessListener<List<ImageLabel>>() {
                    @Override
                    public void onSuccess(List<ImageLabel> labels) {
                        ImageLabel randomLabel = labels.get(DopeBuilder.this.generator.nextInt(labels.size()));
                        String imageLabel = randomLabel.getText();

                        float confidence = randomLabel.getConfidence();
                        Log.d("dopamine", "ImageLabel - " + imageLabel + ": " + confidence);
                        Notification notification = DopeBuilder.this.getImageNotification(type, contact[0], contact[1], image, imageLabel);
                        DopeBuilder.this.scheduleNotification(notification, delay);
                    }
                };

                OnFailureListener onFailure = new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Task failed with an exception
                        // ...
                        Log.d("dopamine", "image labelling error");
                        Notification notification = DopeBuilder.this.getImageNotification(type, contact[0], contact[1], image, null);
                        DopeBuilder.this.scheduleNotification(notification, delay);
                    }
                };

                getImageLabel(imageFile, onSuccess, onFailure);

            } else {
                //Can't find image - proceed without one
                Notification notification = DopeBuilder.this.getNotification(type, contact[0], contact[1], latestPost);
                DopeBuilder.this.scheduleNotification(notification, delay);
            }
        } else {
            //Non-image types
            Notification notification = DopeBuilder.this.getNotification(type, contact[0], contact[1], latestPost);
            DopeBuilder.this.scheduleNotification(notification, delay);
        }
    }

    private File getRandomFileFromGallery() {
        File imgFile = null;
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
            imgFile = new File(fileName);
        }
        return imgFile;
    }

    private Bitmap getBitmapFromFile(File imgFile) {


            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inSampleSize = 2;
            bmOptions.inJustDecodeBounds = false;
            Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath(),bmOptions);
            return bitmap;
    }

    private void getImageLabel(File imgFile, OnSuccessListener onSuccessListener, OnFailureListener onFailureListener) {
        try {
            InputImage image = InputImage.fromFilePath(context, Uri.fromFile(imgFile));
            labeler.process(image)
                    .addOnSuccessListener(onSuccessListener)
                    .addOnFailureListener(onFailureListener);
        } catch (IOException e) {
            e.printStackTrace();
            onFailureListener.onFailure(e);
        }
    }



    private String[] getRandomContact() {
        Cursor managedCursor = context.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,null, null, null, null);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> blocked = preferences.getStringSet("block_contacts", new HashSet<String>());
        int size = managedCursor.getCount();

        int tries = 0;
        String[] results = new String[2];
        Random rnd = new Random();

        //TODO: this is super inefficient!
        while(tries++ < size) {
            int index = rnd.nextInt(size);
            managedCursor.moveToPosition(index);

            results[0] = managedCursor.getString(managedCursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
            results[1] = managedCursor.getString(managedCursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI));

            if(blocked.contains(results[0])) {
                continue;
            } else {
                break;
            }

        }
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


    private Notification getNotification(String type, String name, String userImage, String latestPost) {

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


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId("dopamine_hit");
        }

        return builder.build();
    }

    public Bitmap getResizedBitmap(Bitmap bm, int width ) {
        float aspectRatio = bm.getWidth() /
                (float) bm.getHeight();
        int height = Math.round(width / aspectRatio);

        Bitmap resizedBitmap = Bitmap.createScaledBitmap(
                bm, width, height, false);

        return resizedBitmap;
    }

    private Notification getImageNotification(String type, String name, String userImage, Bitmap recentImage, String recentImageLabel) {

        Notification.Builder builder = new Notification.Builder(this.context);
        String text;
        String actor = name;
        int others = this.generator.nextInt(13) - 7;
        if(others > 0) {
            actor += " and " + others + " others";
        }


        switch(type) {
            case "image_like":
                builder.setContentTitle(actor);
                builder.setContentText("liked your photo");
                builder.setSmallIcon(R.drawable.ic_favorite);
                break;
            case "image_reply":
                builder.setContentTitle(name);
                builder.setSmallIcon(R.drawable.comment);
                text = "commented: "  + instaReplies[this.generator.nextInt(instaReplies.length)].replace("%s", recentImageLabel != null ? " " + recentImageLabel.toLowerCase() : " photo");
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

            builder.setStyle(new Notification.BigPictureStyle()
                    .bigPicture(getResizedBitmap(recentImage, 500)));


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId("dopamine_hit");
        }

        return builder.build();
    }
}
