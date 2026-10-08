package com.example.workscheduler;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class Username extends BaseActivity {

    private EditText etUsername;
    private Button btnSave;
    private DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dbHelper = new DBHelper(this);

        // Check if username already exists
        String existingUsername = dbHelper.getUsername();
        if (existingUsername != null) {
            goToHome(existingUsername);
            return;
        }

        setContentView(R.layout.activity_username);

        etUsername = findViewById(R.id.etUsername);
        btnSave = findViewById(R.id.btnSave);

        // Animate entrance
        View formCard = findViewById(R.id.formCard);

        // Form card: slide up + fade
        formCard.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(350)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        // Button: slide up + fade
        btnSave.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(500)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        btnSave.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            if (!username.isEmpty()) {
                dbHelper.saveUsername(username);
                Toast.makeText(this, "Welcome, " + username + "!", Toast.LENGTH_SHORT).show();
                goToHome(username);
            } else {
                // Shake animation on error
                etUsername.animate()
                        .translationX(10f)
                        .setDuration(50)
                        .withEndAction(() -> etUsername.animate()
                                .translationX(-10f)
                                .setDuration(50)
                                .withEndAction(() -> etUsername.animate()
                                        .translationX(0f)
                                        .setDuration(50)
                                        .start())
                                .start())
                        .start();
                Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void goToHome(String username) {
        Intent intent = new Intent(Username.this, Homepage.class);
        intent.putExtra("username", username);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        finish();
    }
}
