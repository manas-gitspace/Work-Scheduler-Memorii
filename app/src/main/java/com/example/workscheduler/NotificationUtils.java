package com.example.workscheduler;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;

public class NotificationUtils {

    private static final String CHANNEL_ID = "task_channel";
    private static final String TAG = "NotificationUtils";

    public static void ensureChannel(Context context) {
        NotificationManager systemManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && systemManager != null) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Task Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for task reminders");
            systemManager.createNotificationChannel(channel);
        }
    }

    /**
     * Show a notification that opens Homepage when tapped.
     * @param context context
     * @param title notification title
     * @param message notification text
     * @param notifId notification id for notify()
     * @param taskId task id to include in extras (useful to open task)
     */
    public static void showNotification(Context context, String title, String message, int notifId, int taskId) {
        ensureChannel(context);

        Intent openIntent = new Intent(context, Homepage.class);
        openIntent.putExtra("from_notification", true);
        openIntent.putExtra("taskId", taskId);
        // unique action helps PendingIntent uniqueness when different taskIds are used
        openIntent.setAction("open_task_" + taskId);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addNextIntentWithParentStack(openIntent);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) flags |= PendingIntent.FLAG_IMMUTABLE;

        PendingIntent pendingIntent = stackBuilder.getPendingIntent(notifId, flags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_schedule_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        // permission check for Android 13+
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                        == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(notifId, builder.build());
        } else {
            Log.w(TAG, "POST_NOTIFICATIONS permission not granted; skipping notify()");
        }
    }
}
