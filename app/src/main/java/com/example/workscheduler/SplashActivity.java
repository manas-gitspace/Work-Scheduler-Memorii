package com.example.workscheduler;

import android.content.Intent;
import android.os.Bundle;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        View logo = findViewById(R.id.splashLogo);
        TextView title = findViewById(R.id.splashTitle);
        TextView subtitle = findViewById(R.id.splashSubtitle);
        Button btn = findViewById(R.id.splashGetStartedBtn);

        // Initial states for animation
        logo.setAlpha(0f);
        logo.setScaleX(0.8f);
        logo.setScaleY(0.8f);
        title.setAlpha(0f);
        title.setTranslationY(30f);
        subtitle.setAlpha(0f);
        subtitle.setTranslationY(30f);
        btn.setAlpha(0f);
        btn.setTranslationY(50f);

        // Animate elements sequentially
        logo.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(800)
                .setInterpolator(new OvershootInterpolator()).setStartDelay(200).start();

        title.animate().alpha(1f).translationY(0f).setDuration(600)
                .setInterpolator(new DecelerateInterpolator()).setStartDelay(500).start();

        subtitle.animate().alpha(1f).translationY(0f).setDuration(600)
                .setInterpolator(new DecelerateInterpolator()).setStartDelay(700).start();

        DBHelper dbHelper = new DBHelper(SplashActivity.this);
        String username = dbHelper.getUsername();
        boolean isReturningUser = (username != null && !username.trim().isEmpty());

        createNotificationChannel();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        if (isReturningUser) {
            btn.setVisibility(android.view.View.GONE);
            new android.os.Handler().postDelayed(() -> {
                startActivity(new Intent(SplashActivity.this, Homepage.class));
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }, 3000); // 6 seconds for beautiful animation
        } else {
            btn.setVisibility(android.view.View.VISIBLE);
            btn.setAlpha(0f);
            btn.setTranslationY(50f);
            btn.animate().alpha(1f).translationY(0f).setDuration(600)
                    .setInterpolator(new OvershootInterpolator(1.2f)).setStartDelay(900).start();
                    
            btn.setOnClickListener(v -> {
                startActivity(new Intent(SplashActivity.this, Username.class));
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }
    }

    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(
                    "task_channel", "Task Reminders", android.app.NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Shows notifications for scheduled tasks");
            android.app.NotificationManager nm = getSystemService(android.app.NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }
}
