package com.example.workscheduler;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatButton;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class EditScheduleActivity extends BaseActivity {

    private EditText editTitle;
    private TextView textStartTime, textEndTime;
    private SwitchMaterial switchFullDay, switchAlarm;
    private Spinner spinnerPriority;
    private AppCompatButton btnSaveSchedule;

    private ScheduleViewModel scheduleViewModel;
    private ScheduleEntity existingSchedule;
    
    private Calendar startCalendar = Calendar.getInstance();
    private Calendar endCalendar = Calendar.getInstance();
    private String scheduleDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_schedule);

        editTitle = findViewById(R.id.editTitle);
        textStartTime = findViewById(R.id.textStartTime);
        textEndTime = findViewById(R.id.textEndTime);
        switchFullDay = findViewById(R.id.switchFullDay);
        switchAlarm = findViewById(R.id.switchAlarm);
        spinnerPriority = findViewById(R.id.spinnerPriority);
        btnSaveSchedule = findViewById(R.id.btnSaveSchedule);

        scheduleViewModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication())
        ).get(ScheduleViewModel.class);

        // Setup Priority Spinner
        String[] priorities = {"Critical", "High", "Medium", "Low"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, priorities);
        spinnerPriority.setAdapter(adapter);
        spinnerPriority.setSelection(2); // Medium default

        scheduleDate = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date());

        // Check if editing existing or creating new
        if (getIntent().hasExtra("schedule")) {
            existingSchedule = (ScheduleEntity) getIntent().getSerializableExtra("schedule");
            if (existingSchedule != null) {
                editTitle.setText(existingSchedule.getTitle());
                startCalendar.setTimeInMillis(existingSchedule.getStartTimeMillis());
                endCalendar.setTimeInMillis(existingSchedule.getEndTimeMillis());
                switchFullDay.setChecked(existingSchedule.isFullDay());
                switchAlarm.setChecked(existingSchedule.isAlarmEnabled());
                scheduleDate = existingSchedule.getDate();
                
                String priority = existingSchedule.getPriority();
                if (priority != null) {
                    for (int i = 0; i < priorities.length; i++) {
                        if (priorities[i].equalsIgnoreCase(priority)) {
                            spinnerPriority.setSelection(i);
                            break;
                        }
                    }
                }
            }
        } else if (getIntent().hasExtra("preset_hour")) {
            int presetHour = getIntent().getIntExtra("preset_hour", 8);
            startCalendar.set(Calendar.HOUR_OF_DAY, presetHour);
            startCalendar.set(Calendar.MINUTE, 0);
            endCalendar.set(Calendar.HOUR_OF_DAY, presetHour + 1);
            endCalendar.set(Calendar.MINUTE, 0);
            switchAlarm.setChecked(true);
            
            if (getIntent().hasExtra("target_date")) {
                scheduleDate = getIntent().getStringExtra("target_date");
            }
        }

        updateTimeDisplays();

        textStartTime.setOnClickListener(v -> showTimePicker(startCalendar, true));
        textEndTime.setOnClickListener(v -> showTimePicker(endCalendar, false));

        btnSaveSchedule.setOnClickListener(v -> saveSchedule());

        // Interactive Tutorial
        android.content.SharedPreferences prefs = getSharedPreferences("WorkSchedulerPrefs", MODE_PRIVATE);
        if (prefs.getBoolean("isFirstTimeScheduleTutorial", true)) {
            prefs.edit().putBoolean("isFirstTimeScheduleTutorial", false).apply();
            
            android.webkit.WebView tutorialScheduleWebView = findViewById(R.id.tutorialScheduleWebView);
            if (tutorialScheduleWebView != null) {
                tutorialScheduleWebView.setVisibility(View.VISIBLE);
                android.webkit.WebSettings settings = tutorialScheduleWebView.getSettings();
                settings.setJavaScriptEnabled(true);
                settings.setDomStorageEnabled(true);
                tutorialScheduleWebView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                
                tutorialScheduleWebView.addJavascriptInterface(new Object() {
                    @android.webkit.JavascriptInterface
                    public void closeTutorial() {
                        runOnUiThread(() -> {
                            tutorialScheduleWebView.animate().translationY(2000).alpha(0).setDuration(500)
                                .withEndAction(() -> tutorialScheduleWebView.setVisibility(View.GONE)).start();
                        });
                    }
                    
                    @android.webkit.JavascriptInterface
                    public void onStepChanged(int step) {
                        runOnUiThread(() -> {
                            android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
                            float density = metrics.density;
                            float screenHeight = metrics.heightPixels;
                            float webViewHeight = 400 * density;
                            float ty = (screenHeight / 2f) - (webViewHeight / 2f) - (40f * density);
                            tutorialScheduleWebView.animate().translationY(ty).translationX(0)
                                .setDuration(600).setInterpolator(new android.view.animation.OvershootInterpolator()).start();
                        });
                    }
                }, "Android");
                
                tutorialScheduleWebView.loadUrl("file:///android_asset/tutorial_schedule.html");
            }
        }
    }

    private void showTimePicker(Calendar targetCalendar, boolean isStart) {
        TimePickerDialog picker = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            targetCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            targetCalendar.set(Calendar.MINUTE, minute);
            
            // Auto-adjust end time if start time is pushed past it
            if (isStart && startCalendar.getTimeInMillis() >= endCalendar.getTimeInMillis()) {
                endCalendar.setTimeInMillis(startCalendar.getTimeInMillis() + (60 * 60 * 1000)); // +1 hour
            }
            updateTimeDisplays();
        }, targetCalendar.get(Calendar.HOUR_OF_DAY), targetCalendar.get(Calendar.MINUTE), true);
        picker.show();
    }

    private void updateTimeDisplays() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        textStartTime.setText(sdf.format(startCalendar.getTime()));
        textEndTime.setText(sdf.format(endCalendar.getTime()));
        updateDynamicBackground();
    }
    
    private void updateDynamicBackground() {
        View root = findViewById(R.id.editScheduleRoot);
        if (root == null) return;
        
        int hour = startCalendar.get(Calendar.HOUR_OF_DAY);
        if (hour >= 6 && hour < 12) {
            root.setBackgroundResource(R.drawable.bg_time_morning);
        } else if (hour >= 12 && hour < 17) {
            root.setBackgroundResource(R.drawable.bg_time_afternoon);
        } else if (hour >= 17 && hour < 20) {
            root.setBackgroundResource(R.drawable.bg_time_evening);
        } else {
            root.setBackgroundResource(R.drawable.bg_time_night);
        }
    }

    private void saveSchedule() {
        String title = editTitle.getText().toString().trim();
        if (title.isEmpty()) {
            editTitle.setError("Title is required");
            return;
        }

        ScheduleEntity schedule = (existingSchedule != null) ? existingSchedule : new ScheduleEntity();
        schedule.setTitle(title);
        schedule.setDate(scheduleDate);
        schedule.setStartTimeMillis(startCalendar.getTimeInMillis());
        schedule.setEndTimeMillis(endCalendar.getTimeInMillis());
        schedule.setFullDay(switchFullDay.isChecked());
        schedule.setAlarmEnabled(switchAlarm.isChecked());
        schedule.setPriority(spinnerPriority.getSelectedItem().toString());

        if (existingSchedule != null) {
            scheduleViewModel.update(schedule);
            Toast.makeText(this, "Schedule updated", Toast.LENGTH_SHORT).show();
        } else {
            scheduleViewModel.insert(schedule);
            Toast.makeText(this, "Schedule added", Toast.LENGTH_SHORT).show();
        }

        android.content.SharedPreferences prefs = getSharedPreferences("WorkSchedulerPrefs", MODE_PRIVATE);
        prefs.edit().putBoolean("hasCreatedFirstSchedule", true).apply();

        finish();
    }
}
