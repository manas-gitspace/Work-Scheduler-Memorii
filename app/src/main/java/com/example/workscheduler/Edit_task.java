package com.example.workscheduler;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import android.media.RingtoneManager;
import android.net.Uri;
import android.media.Ringtone;
import androidx.annotation.Nullable;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Edit_task extends BaseActivity {
    private EditText taskNameEditText;
    private TextView taskTimeText, taskDateText;
    private Button saveButton;
    private TextView chipCritical, chipHigh, chipMedium, chipLow;
    private LinearLayout timePickerCard, datePickerCard;

    private String[] priorities = {"Critical", "High", "Medium", "Low"};
    private int priorityIndex = 1; // default "High"
    private int taskId;
    private boolean isEditing = false;
    private TaskEntity originalTask;
    private Calendar selectedTimeCalendar = Calendar.getInstance();
    private Calendar selectedDateCalendar = Calendar.getInstance();

    private MaterialSwitch switchReminder;
    private LinearLayout ringtoneSelector;
    private TextView textSelectedRingtone;
    private boolean isAlarmEnabled = true;
    private String selectedRingtoneUri = null;
    private static final int RINGTONE_PICKER_REQUEST = 1001;
    private static final int AUDIO_PICKER_REQUEST = 1002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_task);

        // Back button
        View backButton = findViewById(R.id.backButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                finish();
                overridePendingTransition(R.anim.fade_in, R.anim.slide_out_left);
            });
        }

        // Header title
        TextView headerTitle = findViewById(R.id.headerTitle);

        taskNameEditText = findViewById(R.id.taskNameEditText);
        taskTimeText = findViewById(R.id.taskTimeText);
        taskDateText = findViewById(R.id.taskDateText);
        chipCritical = findViewById(R.id.chipCritical);
        chipHigh = findViewById(R.id.chipHigh);
        chipMedium = findViewById(R.id.chipMedium);
        chipLow = findViewById(R.id.chipLow);
        saveButton = findViewById(R.id.saveButton);
        timePickerCard = findViewById(R.id.timePickerCard);
        datePickerCard = findViewById(R.id.datePickerCard);
        switchReminder = findViewById(R.id.switchReminder);
        ringtoneSelector = findViewById(R.id.ringtoneSelector);
        textSelectedRingtone = findViewById(R.id.textSelectedRingtone);

        setupStaggeredAnimations();

        // Set default time to current time
        updateTimeText();
        updateDateText();

        // Check if we're editing an existing task
        originalTask = (TaskEntity) getIntent().getSerializableExtra("task");
        if (originalTask != null) {
            isEditing = true;
            taskId = originalTask.getId();
            taskNameEditText.setText(originalTask.getName());

            // Parse the existing time
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
                Date date = sdf.parse(originalTask.getTime());
                if (date != null) {
                    selectedTimeCalendar.setTime(date);
                    updateTimeText();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Parse the existing date
            try {
                SimpleDateFormat dateSdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                Date date = dateSdf.parse(originalTask.getDate());
                if (date != null) {
                    selectedDateCalendar.setTime(date);
                    updateDateText();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Set alarm and ringtone
            isAlarmEnabled = originalTask.isAlarmEnabled();
            if (switchReminder != null) switchReminder.setChecked(isAlarmEnabled);
            selectedRingtoneUri = originalTask.getAlarmRingtoneUri();
            updateRingtoneText();

            setPriorityFromString(originalTask.getPriority());
            updatePriorityChips();
            saveButton.setText("Update Task");
            if (headerTitle != null) headerTitle.setText("Edit Task");
        } else {
            // Creating new task
            isEditing = false;
            taskId = (int) System.currentTimeMillis();
            updatePriorityChips();
            saveButton.setText("Save Task");
            if (headerTitle != null) headerTitle.setText("New Task");
        }

        // Make entire cards clickable to open pickers
        timePickerCard.setOnClickListener(v -> showTimePicker());
        datePickerCard.setOnClickListener(v -> showDatePicker());

        chipCritical.setOnClickListener(v -> setPriority("Critical"));
        chipHigh.setOnClickListener(v -> setPriority("High"));
        chipMedium.setOnClickListener(v -> setPriority("Medium"));
        chipLow.setOnClickListener(v -> setPriority("Low"));

        if (switchReminder != null) {
            switchReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
                isAlarmEnabled = isChecked;
                ringtoneSelector.setAlpha(isChecked ? 1.0f : 0.5f);
                ringtoneSelector.setEnabled(isChecked);
            });
        }

        if (ringtoneSelector != null) {
            ringtoneSelector.setOnClickListener(v -> {
                String[] options = {"System Ringtone", "Local Audio File"};
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                        .setTitle("Select Ringtone Source")
                        .setItems(options, (dialog, which) -> {
                            if (which == 0) {
                                Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
                                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Ringtone");
                                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
                                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
                                if (selectedRingtoneUri != null) {
                                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(selectedRingtoneUri));
                                }
                                startActivityForResult(intent, RINGTONE_PICKER_REQUEST);
                            } else if (which == 1) {
                                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                                intent.addCategory(Intent.CATEGORY_OPENABLE);
                                intent.setType("audio/*");
                                startActivityForResult(intent, AUDIO_PICKER_REQUEST);
                            }
                        })
                        .show();
            });
        }

        saveButton.setOnClickListener(v -> {
            String name = taskNameEditText.getText().toString().trim();
            String time = taskTimeText.getText().toString().trim();
            String priority = priorities[priorityIndex];

            if (name.isEmpty() || time.isEmpty() || time.equalsIgnoreCase("Not set")) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check notification permission for Android 13+
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
                    return; // wait for user response
                }
            }

            if (isEditing) {
                // Editing existing task
                saveEditedTask(originalTask, name, time, priority);
            } else {
                // Creating new task
                saveNewTask(name, time, priority);
            }
        });

        // Interactive Tutorial
        android.content.SharedPreferences prefs = getSharedPreferences("WorkSchedulerPrefs", MODE_PRIVATE);
        if (prefs.getBoolean("isFirstTimeTaskTutorial", true)) {
            prefs.edit().putBoolean("isFirstTimeTaskTutorial", false).apply();
            
            android.webkit.WebView tutorialTaskWebView = findViewById(R.id.tutorialTaskWebView);
            if (tutorialTaskWebView != null) {
                tutorialTaskWebView.setVisibility(View.VISIBLE);
                android.webkit.WebSettings settings = tutorialTaskWebView.getSettings();
                settings.setJavaScriptEnabled(true);
                settings.setDomStorageEnabled(true);
                tutorialTaskWebView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                
                tutorialTaskWebView.addJavascriptInterface(new Object() {
                    @android.webkit.JavascriptInterface
                    public void closeTutorial() {
                        runOnUiThread(() -> {
                            tutorialTaskWebView.animate().translationY(2000).alpha(0).setDuration(500)
                                .withEndAction(() -> tutorialTaskWebView.setVisibility(View.GONE)).start();
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
                            tutorialTaskWebView.animate().translationY(ty).translationX(0)
                                .setDuration(600).setInterpolator(new android.view.animation.OvershootInterpolator()).start();
                        });
                    }
                }, "Android");
                
                tutorialTaskWebView.loadUrl("file:///android_asset/tutorial_task.html");
            }
        }
    }

    private void updateTimeText() {
        String formatted = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(selectedTimeCalendar.getTime());
        taskTimeText.setText(formatted);
    }

    private void updateDateText() {
        String formatted = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(selectedDateCalendar.getTime());
        taskDateText.setText(formatted);
    }

    private void saveNewTask(String name, String time, String priority) {
        // Use selected date
        String dateStr = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(selectedDateCalendar.getTime());

        Intent resultIntent = new Intent();
        resultIntent.putExtra("task_title", name);
        resultIntent.putExtra("task_time", time);
        resultIntent.putExtra("task_date", dateStr);
        resultIntent.putExtra("task_priority", priority);
        resultIntent.putExtra("task_id", taskId);
        
        // Pass alarm data via intent (the caller Homepage will create the TaskEntity)
        resultIntent.putExtra("task_alarm_enabled", isAlarmEnabled);
        resultIntent.putExtra("task_ringtone_uri", selectedRingtoneUri);

        setResult(RESULT_OK, resultIntent);

        // ✅ FIXED: Calculate proper trigger time based on selected time
        long triggerTime = calculateTriggerTime();
        if (isAlarmEnabled && triggerTime > 0) {
            NotificationScheduler.scheduleNotification(
                    this,
                    triggerTime,
                    name,
                    "Reminder: " + name + " at " + time,
                    priority,
                    taskId,
                    selectedRingtoneUri
            );
        }

        Toast.makeText(this, "Task saved with " + priority + " priority", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void saveEditedTask(TaskEntity originalTask, String name, String time, String priority) {
        // Update the original task with new data
        String dateStr = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(selectedDateCalendar.getTime());
        originalTask.setName(name);
        originalTask.setTime(time);
        originalTask.setDate(dateStr);
        originalTask.setPriority(priority);
        originalTask.setCritical(priority.equals("Critical"));
        originalTask.setAlarmEnabled(isAlarmEnabled);
        originalTask.setAlarmRingtoneUri(selectedRingtoneUri);

        Intent resultIntent = new Intent();
        resultIntent.putExtra("updated_task", originalTask);
        setResult(RESULT_OK, resultIntent);

        // ✅ FIXED: Calculate proper trigger time for edited task
        long triggerTime = calculateTriggerTime();
        NotificationScheduler.cancelNotification(this, originalTask.getId());
        if (isAlarmEnabled && triggerTime > 0) {
            if (!originalTask.isCompleted()) {
                NotificationScheduler.scheduleNotification(
                        this,
                        triggerTime,
                        name,
                        "Reminder: " + name + " at " + time,
                        priority,
                        originalTask.getId(),
                        selectedRingtoneUri
                );
            }
        }

        Toast.makeText(this, "Task updated successfully!", Toast.LENGTH_SHORT).show();
        finish();
    }

    // ✅ FIXED: Proper time calculation method
    private long calculateTriggerTime() {
        try {
            Calendar notificationTime = (Calendar) selectedTimeCalendar.clone();

            // Set the notification time for today
            Calendar now = Calendar.getInstance();
            notificationTime.set(Calendar.YEAR, now.get(Calendar.YEAR));
            notificationTime.set(Calendar.MONTH, now.get(Calendar.MONTH));
            notificationTime.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));

            // If the selected time has already passed today, schedule for tomorrow
            if (notificationTime.before(now)) {
                notificationTime.add(Calendar.DAY_OF_MONTH, 1);
            }

            return notificationTime.getTimeInMillis();

        } catch (Exception e) {
            e.printStackTrace();
            // Fallback: schedule for 1 minute from now
            return System.currentTimeMillis() + 60000;
        }
    }

    private void updateRingtoneText() {
        if (textSelectedRingtone != null) {
            if (selectedRingtoneUri != null) {
                try {
                    Uri uri = Uri.parse(selectedRingtoneUri);
                    Ringtone ringtone = RingtoneManager.getRingtone(this, uri);
                    if (ringtone != null) {
                        textSelectedRingtone.setText(ringtone.getTitle(this));
                    } else {
                        textSelectedRingtone.setText("Selected Ringtone");
                    }
                } catch (Exception e) {
                    textSelectedRingtone.setText("Custom Ringtone");
                }
            } else {
                textSelectedRingtone.setText("Default Ringtone");
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RINGTONE_PICKER_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (uri != null) {
                selectedRingtoneUri = uri.toString();
            } else {
                selectedRingtoneUri = null; // user selected silent or default
            }
            updateRingtoneText();
        } else if (requestCode == AUDIO_PICKER_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                // Take persistable permission so the app can access it later when the alarm rings
                int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION);
                getContentResolver().takePersistableUriPermission(uri, takeFlags);
                selectedRingtoneUri = uri.toString();
            }
            updateRingtoneText();
        }
    }

    private void showTimePicker() {
        com.google.android.material.timepicker.MaterialTimePicker picker = new com.google.android.material.timepicker.MaterialTimePicker.Builder()
                .setTimeFormat(com.google.android.material.timepicker.TimeFormat.CLOCK_12H)
                .setHour(selectedTimeCalendar.get(Calendar.HOUR_OF_DAY))
                .setMinute(selectedTimeCalendar.get(Calendar.MINUTE))
                .setTitleText("Select Time")
                .build();

        picker.addOnPositiveButtonClickListener(v -> {
            selectedTimeCalendar.set(Calendar.HOUR_OF_DAY, picker.getHour());
            selectedTimeCalendar.set(Calendar.MINUTE, picker.getMinute());
            selectedTimeCalendar.set(Calendar.SECOND, 0);
            updateTimeText();
        });

        picker.show(getSupportFragmentManager(), "TIME_PICKER");
    }

    private void showDatePicker() {
        com.google.android.material.datepicker.MaterialDatePicker<Long> datePicker = com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Date")
                .setSelection(selectedDateCalendar.getTimeInMillis())
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            // MaterialDatePicker uses UTC, adjust to local timezone to prevent off-by-one errors
            Calendar utc = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
            utc.setTimeInMillis(selection);
            
            selectedDateCalendar.set(Calendar.YEAR, utc.get(Calendar.YEAR));
            selectedDateCalendar.set(Calendar.MONTH, utc.get(Calendar.MONTH));
            selectedDateCalendar.set(Calendar.DAY_OF_MONTH, utc.get(Calendar.DAY_OF_MONTH));
            
            updateDateText();
        });

        datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
    }

    private void setupStaggeredAnimations() {
        View animHeader = findViewById(R.id.animHeader);
        View animTaskName = findViewById(R.id.animTaskName);
        View animDateTime = findViewById(R.id.animDateTime);
        View animPriority = findViewById(R.id.animPriority);

        if (animHeader != null) {
            animHeader.setTranslationY(-30f);
            animHeader.animate().alpha(1f).translationY(0f).setDuration(500).setStartDelay(100)
                    .setInterpolator(new android.view.animation.OvershootInterpolator()).start();
        }
        if (animTaskName != null) {
            animTaskName.setTranslationY(30f);
            animTaskName.animate().alpha(1f).translationY(0f).setDuration(500).setStartDelay(250)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
        }
        if (animDateTime != null) {
            animDateTime.setTranslationY(30f);
            animDateTime.animate().alpha(1f).translationY(0f).setDuration(500).setStartDelay(350)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
        }
        if (animPriority != null) {
            animPriority.setTranslationY(30f);
            animPriority.animate().alpha(1f).translationY(0f).setDuration(500).setStartDelay(450)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
        }
        
        View animReminder = findViewById(R.id.animReminder);
        if (animReminder != null) {
            animReminder.setTranslationY(30f);
            animReminder.animate().alpha(1f).translationY(0f).setDuration(500).setStartDelay(500)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
        }
        if (saveButton != null) {
            saveButton.setTranslationY(50f);
            saveButton.animate().alpha(1f).translationY(0f).setDuration(500).setStartDelay(550)
                    .setInterpolator(new android.view.animation.OvershootInterpolator(1.2f)).start();
        }
    }

    private void setPriority(String priority) {
        setPriorityFromString(priority);
        updatePriorityChips();
    }

    private void updatePriorityChips() {
        android.util.TypedValue typedValue = new android.util.TypedValue();
        getTheme().resolveAttribute(R.attr.customTextSecondary, typedValue, true);
        int defaultColor = typedValue.data;

        // Reset all chips
        chipCritical.setBackgroundResource(R.drawable.glass_card);
        chipCritical.setTextColor(defaultColor);
        
        chipHigh.setBackgroundResource(R.drawable.glass_card);
        chipHigh.setTextColor(defaultColor);
        
        chipMedium.setBackgroundResource(R.drawable.glass_card);
        chipMedium.setTextColor(defaultColor);
        
        chipLow.setBackgroundResource(R.drawable.glass_card);
        chipLow.setTextColor(defaultColor);

        // Highlight selected chip with mild scale animation
        TextView activeChip = null;
        int activeColor = 0;

        String priority = priorities[priorityIndex];
        switch (priority) {
            case "Critical": 
                activeChip = chipCritical; 
                activeColor = R.color.accent_rose; 
                break;
            case "High": 
                activeChip = chipHigh; 
                activeColor = R.color.accent_gold; 
                break;
            case "Medium": 
                activeChip = chipMedium; 
                activeColor = R.color.accent_lilac; 
                break;
            case "Low": 
                activeChip = chipLow; 
                activeColor = R.color.accent_teal; 
                break;
        }

        if (activeChip != null) {
            final TextView finalActiveChip = activeChip;
            activeChip.setBackgroundResource(R.drawable.glass_card_active);
            activeChip.setTextColor(getResources().getColor(activeColor, getTheme()));
            activeChip.animate().scaleX(1.05f).scaleY(1.05f).setDuration(200)
                    .withEndAction(() -> finalActiveChip.animate().scaleX(1f).scaleY(1f).setDuration(150).start()).start();
        }
    }

    private void setPriorityFromString(String p) {
        for (int i = 0; i < priorities.length; i++) {
            if (priorities[i].equalsIgnoreCase(p)) {
                priorityIndex = i;
                return;
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.fade_in, R.anim.slide_out_left);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, try saving again
                saveButton.performClick();
            } else {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}