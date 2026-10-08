package com.example.workscheduler;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

public class onboarding extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        View image = findViewById(R.id.onboardingImage);
        View card = findViewById(R.id.onboardingCard);
        View button = findViewById(R.id.nextBtn);

        // Illustration: scale up + fade in
        image.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(600)
                .setStartDelay(200)
                .setInterpolator(new OvershootInterpolator(1.1f))
                .start();

        // Glass card: slide up + fade in
        card.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(450)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        // Button: slide up + fade in
        button.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(650)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        button.setOnClickListener(v -> {
            startActivity(new Intent(onboarding.this, Username.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
            finish();
        });
    }
}
