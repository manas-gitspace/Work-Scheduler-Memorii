package com.example.workscheduler;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AlarmActivity extends AppCompatActivity {
    private static final String TAG = "AlarmActivity";
    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private ObjectAnimator pulseAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Turn on screen and show over lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        setContentView(R.layout.activity_alarm);

        // Get Intent Extras
        String title = getIntent().getStringExtra("title");
        String message = getIntent().getStringExtra("message");
        int taskId = getIntent().getIntExtra("taskId", 0);
        String priority = getIntent().getStringExtra("priority");
        String ringtoneUriString = getIntent().getStringExtra("ringtoneUri");

        // UI Binding
        TextView alarmTitle = findViewById(R.id.alarmTitle);
        TextView alarmTime = findViewById(R.id.alarmTime);
        TextView alarmPriority = findViewById(R.id.alarmPriority);
        Button btnSnooze = findViewById(R.id.btnSnooze);
        Button btnDismiss = findViewById(R.id.btnDismiss);
        View alarmIconContainer = findViewById(R.id.alarmIconContainer);

        // Set Values
        if (title != null) alarmTitle.setText(title);
        if (message != null) {
            // "Reminder: title at 10:00 AM" -> Extract time part or just show message
            String timeStr = message.contains(" at ") ? message.substring(message.lastIndexOf(" at ") + 4) : "Now";
            alarmTime.setText(timeStr);
        }
        if (priority != null) alarmPriority.setText(priority + " Priority");

        // Pulse Animation for the Icon
        pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(
                alarmIconContainer,
                PropertyValuesHolder.ofFloat("scaleX", 1f, 1.15f, 1f),
                PropertyValuesHolder.ofFloat("scaleY", 1f, 1.15f, 1f)
        );
        pulseAnimator.setDuration(1200);
        pulseAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        pulseAnimator.start();

        // Start playing ringtone
        playRingtone(ringtoneUriString);

        // Start vibrating
        startVibration();

        // Button Listeners
        btnDismiss.setOnClickListener(v -> {
            stopAlarm();
            finish();
        });

        btnSnooze.setOnClickListener(v -> {
            stopAlarm();
            // Schedule for 5 minutes later
            long snoozeTime = System.currentTimeMillis() + (5 * 60 * 1000);
            NotificationScheduler.scheduleNotification(
                    this,
                    snoozeTime,
                    title,
                    message,
                    priority,
                    taskId,
                    ringtoneUriString
            );
            finish();
        });
    }

    private void playRingtone(String uriString) {
        Uri uri = null;
        if (uriString != null) {
            uri = Uri.parse(uriString);
        }
        
        // Fallback to default alarm ringtone if none selected or parsing fails
        if (uri == null) {
            uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (uri == null) {
                uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
        }

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, uri);
            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
            mediaPlayer.setAudioAttributes(attributes);
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception e) {
            Log.e(TAG, "Failed to play ringtone", e);
        }
    }

    private void startVibration() {
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pattern = {0, 1000, 1000}; // wait, vibrate, wait
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
            } else {
                vibrator.vibrate(pattern, 0);
            }
        }
    }

    private void stopAlarm() {
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
        }
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (vibrator != null) {
            vibrator.cancel();
            vibrator = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAlarm();
    }
}
