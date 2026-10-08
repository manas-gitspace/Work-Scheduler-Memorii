package com.example.workscheduler;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.net.Uri;

public class NotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "NotificationReceiver";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra("title");
        String message = intent.getStringExtra("message");
        int taskId = intent.getIntExtra("taskId", 0);
        String priority = intent.getStringExtra("priority");
        String ringtoneUri = intent.getStringExtra("ringtoneUri");

        Log.d(TAG, "onReceive: taskId=" + taskId + " title=" + title);

        // Build and show notification with contentIntent -> Homepage
        createNotification(context, title, message, taskId, ringtoneUri);

    }

    private void createNotification(Context context, String title, String message, int taskId, String ringtoneUri) {
        NotificationUtils.ensureChannel(context);

        // Intent to open Homepage when user taps the notification normally
        Intent openIntent = new Intent(context, Homepage.class);
        openIntent.putExtra("from_notification", true);
        openIntent.putExtra("taskId", taskId);
        openIntent.setAction("open_task_" + taskId);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) flags |= PendingIntent.FLAG_IMMUTABLE;

        PendingIntent contentPendingIntent = PendingIntent.getActivity(
                context,
                taskId,
                openIntent,
                flags
        );

        // Intent for full-screen alarm overlay
        Intent fullScreenIntent = new Intent(context, AlarmActivity.class);
        fullScreenIntent.putExtra("title", title);
        fullScreenIntent.putExtra("message", message);
        fullScreenIntent.putExtra("taskId", taskId);
        fullScreenIntent.putExtra("priority", "High"); // force string since it's an extra
        fullScreenIntent.putExtra("ringtoneUri", ringtoneUri);
        fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                taskId,
                fullScreenIntent,
                flags
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "task_channel")
                .setSmallIcon(R.drawable.ic_schedule_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(contentPendingIntent)
                .setFullScreenIntent(fullScreenPendingIntent, true);
                
        if (ringtoneUri != null) {
            try {
                builder.setSound(Uri.parse(ringtoneUri));
            } catch (Exception e) {
                Log.e(TAG, "Invalid ringtone URI: " + ringtoneUri);
            }
        }

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(taskId, builder.build());
            Log.d(TAG, "Notification shown for taskId=" + taskId);
        }
    }
}
