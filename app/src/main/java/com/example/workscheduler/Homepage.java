package com.example.workscheduler;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashSet;
import java.util.Set;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.stream.Collectors;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.view.HapticFeedbackConstants;
import android.widget.HorizontalScrollView;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.animation.ValueAnimator;
import android.view.animation.DecelerateInterpolator;

public class Homepage extends BaseActivity {

    TextView greetingText, textViewUsername, myTasksLabel, todayCount, pendingCount;
    TaskAdapter taskAdapter;
    TaskViewModel taskViewModel;
    ScheduleViewModel scheduleViewModel;
    ScheduleAdapter scheduleAdapter;
    CalendarAdapter calendarAdapter;
    Calendar displayedMonth = Calendar.getInstance();
    List<TaskEntity> currentTaskList;

    View homeContainer, scheduleContainer;
    TextView scheduleDateText;
    RecyclerView recyclerViewSchedule;
    RecyclerView calendarGrid;
    AnimatedNavBar bottomNavBar;

    private int activeTabIndex = 0;
    private String clipboardDate = null;
    private String selectedRoutineFilter = "all";
    private List<ScheduleEntity> currentSchedulesList = new ArrayList<>();

    private static final int ADD_TASK_REQUEST = 1;
    private static final int EDIT_TASK_REQUEST = 2;
    private boolean showingCompletedTasks = false;

    // Collapsible calendar
    private boolean isCalendarCollapsed = false;
    private View fullCalendarContainer;
    private HorizontalScrollView weekStripScroll;
    private LinearLayout weekStripContainer;
    private View calendarCollapseHandle;

    // Tab sound
    private SoundPool soundPool;
    private int tabClickSoundId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homepage);

        if (savedInstanceState != null) {
            activeTabIndex = savedInstanceState.getInt("active_tab_index", 0);
        }

        greetingText = findViewById(R.id.greetingText);
        textViewUsername = findViewById(R.id.textViewUsername);
        myTasksLabel = findViewById(R.id.myTasksLabel);
        todayCount = findViewById(R.id.todayCount);
        pendingCount = findViewById(R.id.pendingCount);

        homeContainer = findViewById(R.id.homeContainer);
        scheduleContainer = findViewById(R.id.scheduleContainer);
        scheduleDateText = findViewById(R.id.scheduleDateText);
        recyclerViewSchedule = findViewById(R.id.recyclerViewSchedule);
        
        String todayStr = new SimpleDateFormat("EEEE, MMM dd", Locale.getDefault()).format(new Date());
        if (scheduleDateText != null) scheduleDateText.setText(todayStr);

        String username = getIntent().getStringExtra("username");
        if (username == null || username.trim().isEmpty()) {
            DBHelper dbHelper = new DBHelper(this);
            username = dbHelper.getUsername();
        }
        setGreetingMessage(username);

        RecyclerView recyclerView = findViewById(R.id.recyclerViewTasks);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        taskAdapter = new TaskAdapter(this);
        taskAdapter.setOnTaskCheckedChangeListener((task, isChecked) -> {
            task.setCompleted(isChecked);
            taskViewModel.update(task);

            if (isChecked) {
                NotificationScheduler.cancelNotification(Homepage.this, task.getId());
                Toast.makeText(Homepage.this, "Task completed: " + task.getName(), Toast.LENGTH_SHORT).show();
            } else {
                scheduleTaskNotification(task);
                Toast.makeText(Homepage.this, "Task reopened: " + task.getName(), Toast.LENGTH_SHORT).show();
            }

            refreshTaskList();
        });

        taskAdapter.setOnTaskClickListener(this::openTaskForEditing);
        taskAdapter.setOnTaskLongClickListener((task, position) -> {
            String[] options = {"✏️ Edit", "🗑️ Delete"};
            new MaterialAlertDialogBuilder(this, getAlertDialogTheme())
                    .setTitle(task.getName())
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            openTaskForEditing(task);
                        } else {
                            showDeleteConfirmation(task);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
        recyclerView.setAdapter(taskAdapter);

        taskViewModel = new ViewModelProvider(
                this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication())
        ).get(TaskViewModel.class);
        
        scheduleViewModel = new ViewModelProvider(
                this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication())
        ).get(ScheduleViewModel.class);

        taskViewModel.getAllTasks().observe(this, tasks -> {
            if (tasks != null) {
                currentTaskList = tasks;
                updateStats(tasks);
            }
        });
        
        setupScheduleTab();
        setupInAppWidget();

        loadAllTasks();

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        toolbar.setOnMenuItemClickListener(this::onMenuItemClick);
        toolbar.setNavigationOnClickListener(v -> showProfileDialog());
        toolbar.setOnClickListener(v -> showProfileDialog());

        FloatingActionButton fabAdd = findViewById(R.id.fabAddSchedule);
        fabAdd.setOnClickListener(v -> {
            startActivityForResult(new Intent(Homepage.this, Edit_task.class), ADD_TASK_REQUEST);
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        });

        // Entrance animations
        View greetingLayout = findViewById(R.id.greetingLayout);
        greetingLayout.setAlpha(0f);
        greetingLayout.setTranslationY(-20f);
        greetingLayout.animate().alpha(1f).translationY(0f).setDuration(500).setStartDelay(100).start();

        // FAB entrance animation
        fabAdd.setScaleX(0f);
        fabAdd.setScaleY(0f);
        fabAdd.animate().scaleX(1f).scaleY(1f).setDuration(400).setStartDelay(500)
                .setInterpolator(new android.view.animation.OvershootInterpolator(2f)).start();

        // Initialize tab click sound
        AudioAttributes audioAttrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder().setMaxStreams(1).setAudioAttributes(audioAttrs).build();
        tabClickSoundId = soundPool.load(this, R.raw.tab_click, 1);

        // Bottom Nav Bar
        bottomNavBar = findViewById(R.id.bottomNavBar);
        bottomNavBar.setTranslationY(200f);
        bottomNavBar.setAlpha(0f);
        bottomNavBar.animate().translationY(0f).alpha(1f).setDuration(500).setStartDelay(300)
                .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
        bottomNavBar.setOnTabSelectedListener(index -> {
            // Haptic feedback
            bottomNavBar.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            // Sound feedback
            if (soundPool != null && tabClickSoundId > 0) {
                soundPool.play(tabClickSoundId, 0.3f, 0.3f, 1, 0, 1f);
            }
            activeTabIndex = index;
            switch (index) {
                case 0: // Home
                    homeContainer.setVisibility(View.VISIBLE);
                    scheduleContainer.setVisibility(View.GONE);
                    fabAdd.setVisibility(View.VISIBLE);
                    fabAdd.setOnClickListener(v -> {
                        startActivityForResult(new Intent(Homepage.this, Edit_task.class), ADD_TASK_REQUEST);
                        overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
                    });
                    break;
                case 1: // Schedule
                    homeContainer.setVisibility(View.GONE);
                    scheduleContainer.setVisibility(View.VISIBLE);
                    fabAdd.setVisibility(View.VISIBLE);
                    fabAdd.setOnClickListener(v -> {
                        Intent intent = new Intent(Homepage.this, EditScheduleActivity.class);
                        intent.putExtra("target_date", currentScheduleDate);
                        startActivity(intent);
                    });
                    break;
                case 2:
                    Toast.makeText(this, "Notes — Coming Soon", Toast.LENGTH_SHORT).show();
                    break;
                case 3:
                    Toast.makeText(this, "Diary — Coming Soon", Toast.LENGTH_SHORT).show();
                    break;
                case 4:
                    Toast.makeText(this, "Thoughts — Coming Soon", Toast.LENGTH_SHORT).show();
                    break;
            }
        });

        if (activeTabIndex != 0) {
            bottomNavBar.post(() -> bottomNavBar.selectTab(activeTabIndex));
        }

        setupScrollHiding(recyclerView, bottomNavBar);
        setupScrollHiding(recyclerViewSchedule, bottomNavBar);

        // Handle empty state
        View emptyStateLayout = findViewById(R.id.emptyStateLayout);
        taskViewModel.getAllTasks().observe(this, tasks -> {
            if (tasks != null) {
                boolean isEmpty = tasks.isEmpty();
                recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                emptyStateLayout.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            }
        });

        // Interactive Tutorial
        SharedPreferences prefs = getSharedPreferences("WorkSchedulerPrefs", MODE_PRIVATE);
        if (prefs.getBoolean("isFirstTimeTutorial", true)) {
            prefs.edit().putBoolean("isFirstTimeTutorial", false).apply();
            
            android.webkit.WebView tutorialCompanionWebView = findViewById(R.id.tutorialCompanionWebView);
            if (tutorialCompanionWebView != null) {
                tutorialCompanionWebView.setVisibility(View.VISIBLE);
                android.webkit.WebSettings settings = tutorialCompanionWebView.getSettings();
                settings.setJavaScriptEnabled(true);
                settings.setDomStorageEnabled(true);
                tutorialCompanionWebView.setBackgroundColor(Color.TRANSPARENT);
                
                tutorialCompanionWebView.addJavascriptInterface(new Object() {
                    @android.webkit.JavascriptInterface
                    public void closeTutorial() {
                        runOnUiThread(() -> {
                            tutorialCompanionWebView.animate().translationY(2000).alpha(0).setDuration(500)
                                .withEndAction(() -> tutorialCompanionWebView.setVisibility(View.GONE)).start();
                        });
                    }
                    
                    @android.webkit.JavascriptInterface
                    public void onStepChanged(int step) {
                        runOnUiThread(() -> {
                            android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
                            float density = metrics.density;
                            float screenWidth = metrics.widthPixels;
                            float screenHeight = metrics.heightPixels;
                            
                            float webViewWidth = 320 * density;
                            float webViewHeight = 400 * density;
                            
                            float tx = 0, ty = 0;
                            switch(step) {
                                case 0:
                                case 1:
                                case 2:
                                    ty = - (screenHeight / 2f) + (webViewHeight / 2f) + (60f * density);
                                    tx = (screenWidth / 2f) - (webViewWidth / 2f) - (24f * density);
                                    break;
                                case 3:
                                    ty = (screenHeight / 2f) - (webViewHeight / 2f) - (60f * density);
                                    tx = (screenWidth / 2f) - (webViewWidth / 2f) - (120f * density);
                                    break;
                                case 4:
                                    ty = (screenHeight / 2f) - (webViewHeight / 2f) - (100f * density);
                                    tx = 0;
                                    break;
                            }
                            tutorialCompanionWebView.animate().translationX(tx).translationY(ty)
                                .setDuration(600).setInterpolator(new android.view.animation.OvershootInterpolator()).start();
                        });
                    }
                }, "Android");
                
                tutorialCompanionWebView.loadUrl("file:///android_asset/tutorial.html");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getSharedPreferences("WorkSchedulerPrefs", MODE_PRIVATE);
        boolean hasCreatedFirstSchedule = prefs.getBoolean("hasCreatedFirstSchedule", false);
        boolean isFirstTimeDeleteTutorial = prefs.getBoolean("isFirstTimeDeleteScheduleTutorial", true);
        
        if (hasCreatedFirstSchedule && isFirstTimeDeleteTutorial) {
            prefs.edit().putBoolean("isFirstTimeDeleteScheduleTutorial", false).apply();
            
            android.webkit.WebView tutorialCompanionWebView = findViewById(R.id.tutorialCompanionWebView);
            if (tutorialCompanionWebView != null) {
                // Switch to schedule tab for context
                AnimatedNavBar bottomNavBar = findViewById(R.id.bottomNavBar);
                if (bottomNavBar != null) {
                    bottomNavBar.selectTab(1);
                }

                tutorialCompanionWebView.setVisibility(View.VISIBLE);
                tutorialCompanionWebView.setAlpha(1f);
                tutorialCompanionWebView.setTranslationY(0);
                
                android.webkit.WebSettings settings = tutorialCompanionWebView.getSettings();
                settings.setJavaScriptEnabled(true);
                settings.setDomStorageEnabled(true);
                tutorialCompanionWebView.setBackgroundColor(Color.TRANSPARENT);
                
                tutorialCompanionWebView.addJavascriptInterface(new Object() {
                    @android.webkit.JavascriptInterface
                    public void closeTutorial() {
                        runOnUiThread(() -> {
                            tutorialCompanionWebView.animate().translationY(2000).alpha(0).setDuration(500)
                                .withEndAction(() -> tutorialCompanionWebView.setVisibility(View.GONE)).start();
                        });
                    }
                    
                    @android.webkit.JavascriptInterface
                    public void onStepChanged(int step) {
                        runOnUiThread(() -> {
                            android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
                            float density = metrics.density;
                            float screenHeight = metrics.heightPixels;
                            float webViewHeight = 400 * density;
                            
                            float ty = (screenHeight / 2f) - (webViewHeight / 2f) - (100f * density);
                            tutorialCompanionWebView.animate().translationX(0).translationY(ty)
                                .setDuration(600).setInterpolator(new android.view.animation.OvershootInterpolator()).start();
                        });
                    }
                }, "Android");
                
                tutorialCompanionWebView.loadUrl("file:///android_asset/tutorial_delete_schedule.html");
            }
        }

        // Refresh calendar and schedule data when returning
        refreshCalendar();
        loadScheduleDataForDate(currentScheduleDate);
    }
    
    private void setupScrollHiding(RecyclerView rv, View navBar) {
        if (rv == null || navBar == null) return;
        rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@androidx.annotation.NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 10 && navBar.getTranslationY() == 0) { // Scrolling down -> Hide
                    navBar.animate().translationY(navBar.getHeight() + 100).setDuration(250)
                            .setInterpolator(new android.view.animation.AccelerateInterpolator()).start();
                } else if (dy < -10 && navBar.getTranslationY() > 0) { // Scrolling up -> Show
                    navBar.animate().translationY(0).setDuration(250)
                            .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
                }
            }
        });
    }

    private String currentScheduleDate = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date());

    private void setupScheduleTab() {
        if (recyclerViewSchedule == null) return;
        recyclerViewSchedule.setLayoutManager(new LinearLayoutManager(this));
        scheduleAdapter = new ScheduleAdapter(this);
        recyclerViewSchedule.setAdapter(scheduleAdapter);

        // Calendar Grid Setup
        calendarGrid = findViewById(R.id.calendarGrid);
        if (calendarGrid != null) {
            calendarGrid.setLayoutManager(new GridLayoutManager(this, 7));
            calendarAdapter = new CalendarAdapter(date -> {
                currentScheduleDate = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date.getTime());
                updateScheduleDateLabel();
                loadScheduleDataForDate(currentScheduleDate);
            });
            calendarAdapter.setOnDateLongClickListener(date -> {
                View dialogView = getLayoutInflater().inflate(R.layout.dialog_schedule_actions, null);
                TextView dialogSubtitle = dialogView.findViewById(R.id.dialogSubtitle);
                View btnCopyAction = dialogView.findViewById(R.id.btnCopyAction);
                View btnPasteAction = dialogView.findViewById(R.id.btnPasteAction);
                TextView textPasteDesc = dialogView.findViewById(R.id.textPasteDesc);
                View btnCancelAction = dialogView.findViewById(R.id.btnCancelAction);

                String formattedDate = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date.getTime());
                String readableDate = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(date.getTime());
                if (dialogSubtitle != null) dialogSubtitle.setText(readableDate);

                boolean hasEvents = (calendarAdapter != null && calendarAdapter.hasEvents(date));
                boolean hasClipboard = (clipboardDate != null);

                if (!hasEvents) {
                    if (btnCopyAction != null) {
                        btnCopyAction.setAlpha(0.4f);
                        btnCopyAction.setClickable(false);
                        btnCopyAction.setFocusable(false);
                    }
                }
                if (!hasClipboard) {
                    if (btnPasteAction != null) {
                        btnPasteAction.setAlpha(0.4f);
                        btnPasteAction.setClickable(false);
                        btnPasteAction.setFocusable(false);
                    }
                    if (textPasteDesc != null) {
                        textPasteDesc.setText("Clipboard is empty");
                    }
                } else {
                    if (textPasteDesc != null) {
                        textPasteDesc.setText("Paste from " + clipboardDate);
                    }
                }

                androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(Homepage.this, getAlertDialogTheme())
                        .setView(dialogView)
                        .create();

                if (hasEvents && btnCopyAction != null) {
                    btnCopyAction.setOnClickListener(v -> {
                        clipboardDate = formattedDate;
                        Toast.makeText(Homepage.this, "Copied schedule for " + clipboardDate, Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
                }
                if (hasClipboard && btnPasteAction != null) {
                    btnPasteAction.setOnClickListener(v -> {
                        dialog.dismiss();
                        new MaterialAlertDialogBuilder(Homepage.this, getAlertDialogTheme())
                                .setTitle("Paste Schedule")
                                .setMessage("Paste schedule from " + clipboardDate + " to " + formattedDate + "?")
                                .setPositiveButton("Paste", (dialogInner, whichInner) -> {
                                    scheduleViewModel.copyFromDate(clipboardDate, formattedDate);
                                    Toast.makeText(Homepage.this, "Schedule pasted!", Toast.LENGTH_SHORT).show();
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    });
                }
                if (btnCancelAction != null) {
                    btnCancelAction.setOnClickListener(v -> dialog.dismiss());
                }

                dialog.show();
            });
            calendarGrid.setAdapter(calendarAdapter);
            refreshCalendar();

            // Set up swipe gestures for month changing on calendar grid with sliding animations
            GestureDetector gestureDetector = new GestureDetector(Homepage.this, new GestureDetector.SimpleOnGestureListener() {
                private static final int SWIPE_THRESHOLD = 100;
                private static final int SWIPE_VELOCITY_THRESHOLD = 100;

                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                    if (e1 == null || e2 == null) return false;
                    float diffX = e2.getX() - e1.getX();
                    float diffY = e2.getY() - e1.getY();
                    if (Math.abs(diffX) > Math.abs(diffY)) {
                        if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                            if (diffX > 0) {
                                // Swipe right -> previous month
                                changeMonthWithAnimation(-1);
                                return true;
                            } else {
                                // Swipe left -> next month
                                changeMonthWithAnimation(1);
                                return true;
                            }
                        }
                    }
                    return false;
                }
            });

            calendarGrid.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
                @Override
                public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                    gestureDetector.onTouchEvent(e);
                    return false;
                }
            });
        }

        // Month Navigation
        View btnPrevMonth = findViewById(R.id.btnPrevMonth);
        View btnNextMonth = findViewById(R.id.btnNextMonth);
        if (btnPrevMonth != null) btnPrevMonth.setOnClickListener(v -> {
            changeMonthWithAnimation(-1);
        });
        if (btnNextMonth != null) btnNextMonth.setOnClickListener(v -> {
            changeMonthWithAnimation(1);
        });

        updateScheduleDateLabel();

        scheduleAdapter.setListener(schedule -> {
            Intent intent = new Intent(Homepage.this, EditScheduleActivity.class);
            intent.putExtra("schedule", schedule);
            startActivity(intent);
        });

        // Swipe to Delete
        androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback swipeCallback = new androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(0, androidx.recyclerview.widget.ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                ScheduleEntity item = scheduleAdapter.getItem(position);
                if (item != null) {
                    scheduleViewModel.delete(item);
                    Toast.makeText(Homepage.this, "Schedule deleted", Toast.LENGTH_SHORT).show();
                } else {
                    scheduleAdapter.notifyItemChanged(position);
                }
            }
        };
        new androidx.recyclerview.widget.ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerViewSchedule);

        // Routine Tabs Setup
        TextView routineTabAll = findViewById(R.id.routineTabAll);
        TextView routineTabMorning = findViewById(R.id.routineTabMorning);
        TextView routineTabAfternoon = findViewById(R.id.routineTabAfternoon);
        TextView routineTabEvening = findViewById(R.id.routineTabEvening);
        TextView routineTabNight = findViewById(R.id.routineTabNight);

        if (routineTabAll != null) {
            routineTabAll.setOnClickListener(v -> {
                selectedRoutineFilter = "all";
                updateRoutineTabUi();
                applyRoutineFilter();
            });
        }
        if (routineTabMorning != null) {
            routineTabMorning.setOnClickListener(v -> {
                selectedRoutineFilter = "morning";
                updateRoutineTabUi();
                applyRoutineFilter();
            });
        }
        if (routineTabAfternoon != null) {
            routineTabAfternoon.setOnClickListener(v -> {
                selectedRoutineFilter = "afternoon";
                updateRoutineTabUi();
                applyRoutineFilter();
            });
        }
        if (routineTabEvening != null) {
            routineTabEvening.setOnClickListener(v -> {
                selectedRoutineFilter = "evening";
                updateRoutineTabUi();
                applyRoutineFilter();
            });
        }
        if (routineTabNight != null) {
            routineTabNight.setOnClickListener(v -> {
                selectedRoutineFilter = "night";
                updateRoutineTabUi();
                applyRoutineFilter();
            });
        }
        updateRoutineTabUi();

        // Collapsible Calendar Setup
        fullCalendarContainer = findViewById(R.id.fullCalendarContainer);
        weekStripScroll = findViewById(R.id.weekStripScroll);
        weekStripContainer = findViewById(R.id.weekStripContainer);
        calendarCollapseHandle = findViewById(R.id.calendarCollapseHandle);

        if (calendarCollapseHandle != null) {
            calendarCollapseHandle.setOnClickListener(v -> toggleCalendarCollapse());
        }

        loadScheduleDataForDate(currentScheduleDate);
    }

    private void changeMonthWithAnimation(int offset) {
        if (calendarGrid == null || calendarAdapter == null) return;

        float width = calendarGrid.getWidth();
        if (width <= 0) width = 1000f;

        boolean slideOutToLeft = (offset > 0);
        float startTranslation = slideOutToLeft ? -width : width;
        float endTranslation = slideOutToLeft ? width : -width;

        calendarGrid.animate()
                .translationX(startTranslation)
                .setDuration(150)
                .withEndAction(() -> {
                    displayedMonth.add(Calendar.MONTH, offset);
                    refreshCalendar();
                    calendarGrid.setTranslationX(endTranslation);
                    calendarGrid.animate()
                            .translationX(0)
                            .setDuration(150)
                            .start();
                })
                .start();
    }

    private void toggleCalendarCollapse() {
        if (fullCalendarContainer == null || weekStripScroll == null) return;

        if (isCalendarCollapsed) {
            // Expand: show full calendar, hide week strip
            weekStripScroll.animate().alpha(0f).setDuration(200).withEndAction(() -> {
                weekStripScroll.setVisibility(View.GONE);
                fullCalendarContainer.setVisibility(View.VISIBLE);
                fullCalendarContainer.setAlpha(0f);
                fullCalendarContainer.setTranslationY(-50f);
                fullCalendarContainer.animate().alpha(1f).translationY(0f).setDuration(300)
                        .setInterpolator(new DecelerateInterpolator()).start();
            }).start();
            isCalendarCollapsed = false;
        } else {
            // Collapse: hide full calendar, show week strip
            fullCalendarContainer.animate().alpha(0f).translationY(-50f).setDuration(200).withEndAction(() -> {
                fullCalendarContainer.setVisibility(View.GONE);
                populateWeekStrip();
                weekStripScroll.setVisibility(View.VISIBLE);
                weekStripScroll.setAlpha(0f);
                weekStripScroll.animate().alpha(1f).setDuration(300)
                        .setInterpolator(new DecelerateInterpolator()).start();
            }).start();
            isCalendarCollapsed = true;
        }
    }

    private void populateWeekStrip() {
        if (weekStripContainer == null) return;
        weekStripContainer.removeAllViews();

        // Determine the week containing the selected date
        Calendar selectedCal = Calendar.getInstance();
        try {
            selectedCal.setTime(new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).parse(currentScheduleDate));
        } catch (Exception e) {
            selectedCal = Calendar.getInstance();
        }

        // Find the Sunday of this week
        Calendar weekStart = (Calendar) selectedCal.clone();
        weekStart.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);

        Calendar today = Calendar.getInstance();
        SimpleDateFormat dayFmt = new SimpleDateFormat("EEE", Locale.getDefault());
        SimpleDateFormat dateFmt = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

        for (int i = 0; i < 7; i++) {
            Calendar dayCal = (Calendar) weekStart.clone();
            dayCal.add(Calendar.DAY_OF_MONTH, i);

            boolean isToday = (dayCal.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                    && dayCal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR));
            boolean isSelected = dateFmt.format(dayCal.getTime()).equals(currentScheduleDate);

            // Build a vertical mini cell: day name + day number
            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(android.view.Gravity.CENTER);
            int cellSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, getResources().getDisplayMetrics());
            LinearLayout.LayoutParams cellParams = new LinearLayout.LayoutParams(cellSize, LinearLayout.LayoutParams.MATCH_PARENT);
            cellParams.setMargins(4, 0, 4, 0);
            cell.setLayoutParams(cellParams);
            cell.setClickable(true);
            cell.setFocusable(true);

            // Day name (M, T, W...)
            TextView dayNameTv = new TextView(this);
            dayNameTv.setText(dayFmt.format(dayCal.getTime()).substring(0, 1).toUpperCase());
            dayNameTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            dayNameTv.setGravity(android.view.Gravity.CENTER);

            // Day number
            TextView dayNumTv = new TextView(this);
            dayNumTv.setText(String.valueOf(dayCal.get(Calendar.DAY_OF_MONTH)));
            dayNumTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            dayNumTv.setGravity(android.view.Gravity.CENTER);
            dayNumTv.setTypeface(dayNumTv.getTypeface(), android.graphics.Typeface.BOLD);

            // Resolve theme colors
            TypedValue tvPrimary = new TypedValue();
            getTheme().resolveAttribute(R.attr.customTextPrimary, tvPrimary, true);
            TypedValue tvMuted = new TypedValue();
            getTheme().resolveAttribute(R.attr.customTextMuted, tvMuted, true);

            if (isSelected) {
                GradientDrawable selBg = new GradientDrawable();
                selBg.setShape(GradientDrawable.RECTANGLE);
                selBg.setCornerRadius(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14, getResources().getDisplayMetrics()));
                selBg.setColor(0xFFb090f0);
                cell.setBackground(selBg);
                dayNameTv.setTextColor(0xFFFFFFFF);
                dayNumTv.setTextColor(0xFFFFFFFF);
            } else if (isToday) {
                GradientDrawable todayBg = new GradientDrawable();
                todayBg.setShape(GradientDrawable.RECTANGLE);
                todayBg.setCornerRadius(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14, getResources().getDisplayMetrics()));
                todayBg.setStroke((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics()), 0xFFb090f0);
                cell.setBackground(todayBg);
                dayNameTv.setTextColor(tvPrimary.data);
                dayNumTv.setTextColor(tvPrimary.data);
            } else {
                dayNameTv.setTextColor(tvMuted.data);
                dayNumTv.setTextColor(tvPrimary.data);
            }

            cell.addView(dayNameTv);
            cell.addView(dayNumTv);

            final String dateStr = dateFmt.format(dayCal.getTime());
            cell.setOnClickListener(v -> {
                currentScheduleDate = dateStr;
                updateScheduleDateLabel();
                loadScheduleDataForDate(currentScheduleDate);
                populateWeekStrip(); // refresh selection highlight
            });

            weekStripContainer.addView(cell);
        }
    }

    private void refreshCalendar() {
        if (calendarAdapter == null) return;
        List<CalendarAdapter.CalendarDay> days = CalendarAdapter.generateDaysForMonth(displayedMonth);
        int todayPos = CalendarAdapter.findTodayPosition(days);

        // Find selected date position
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        int selectedPos = todayPos;
        for (int i = 0; i < days.size(); i++) {
            String dayStr = sdf.format(days.get(i).calendar.getTime());
            if (dayStr.equals(currentScheduleDate)) {
                selectedPos = i;
                break;
            }
        }

        calendarAdapter.setDays(days, selectedPos);

        // Update month label
        TextView textMonthYear = findViewById(R.id.textMonthYear);
        if (textMonthYear != null) {
            textMonthYear.setText(new SimpleDateFormat("MMM, yyyy", Locale.getDefault()).format(displayedMonth.getTime()).toUpperCase());
        }

        // Load event dots for this month
        loadEventDotsForMonth();
    }

    private void loadEventDotsForMonth() {
        // Query all dates in the displayed month that have schedules
        Calendar start = (Calendar) displayedMonth.clone();
        start.set(Calendar.DAY_OF_MONTH, 1);
        Calendar end = (Calendar) start.clone();
        end.add(Calendar.MONTH, 1);

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        Set<String> datesWithEvents = new HashSet<>();

        // Check each day of the month for schedules
        Calendar day = (Calendar) start.clone();
        while (day.before(end)) {
            String dateStr = sdf.format(day.getTime());
            scheduleViewModel.getSchedulesForDate(dateStr).observe(this, schedules -> {
                if (schedules != null && !schedules.isEmpty()) {
                    datesWithEvents.add(dateStr);
                    if (calendarAdapter != null) calendarAdapter.setDatesWithEvents(datesWithEvents);
                }
            });
            day.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private void updateScheduleDateLabel() {
        if (scheduleDateText != null) {
            try {
                Calendar cal = Calendar.getInstance();
                cal.setTime(new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).parse(currentScheduleDate));
                String label = new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(cal.getTime());
                scheduleDateText.setText(label);
            } catch (Exception e) {
                scheduleDateText.setText(currentScheduleDate);
            }
        }
    }

    private void setupInAppWidget() {
        View widgetContainer = findViewById(R.id.inAppWidgetContainer);
        if (widgetContainer == null) return;

        RecyclerView rv = findViewById(R.id.inAppWidgetRecyclerView);
        View empty = findViewById(R.id.inAppWidgetEmpty);
        TextView count = findViewById(R.id.inAppWidgetCount);
        View btnHide = findViewById(R.id.btnHideInAppWidget);

        android.content.SharedPreferences prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        boolean showWidget = prefs.getBoolean("show_schedule_widget", true);
        widgetContainer.setVisibility(showWidget ? View.VISIBLE : View.GONE);

        btnHide.setOnClickListener(v -> {
            widgetContainer.setVisibility(View.GONE);
            prefs.edit().putBoolean("show_schedule_widget", false).apply();
            Toast.makeText(this, "Widget hidden. Restore via More menu.", Toast.LENGTH_SHORT).show();
        });

        rv.setLayoutManager(new LinearLayoutManager(this));
        InAppWidgetAdapter adapter = new InAppWidgetAdapter(this);
        rv.setAdapter(adapter);

        String today = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Calendar.getInstance().getTime());
        scheduleViewModel.getSchedulesForDate(today).observe(this, schedules -> {
            if (schedules == null || schedules.isEmpty()) {
                rv.setVisibility(View.GONE);
                empty.setVisibility(View.VISIBLE);
                count.setText("0 items");
            } else {
                rv.setVisibility(View.VISIBLE);
                empty.setVisibility(View.GONE);
                count.setText(schedules.size() + (schedules.size() == 1 ? " item" : " items"));
                adapter.setItems(schedules);
            }
        });
    }

    private void loadScheduleDataForDate(String dateStr) {
        scheduleViewModel.getSchedulesForDate(dateStr).observe(this, schedules -> {
            currentSchedulesList = schedules != null ? schedules : new ArrayList<>();
            applyRoutineFilter();
        });
    }

    private void updateRoutineTabUi() {
        TextView routineTabAll = findViewById(R.id.routineTabAll);
        TextView routineTabMorning = findViewById(R.id.routineTabMorning);
        TextView routineTabAfternoon = findViewById(R.id.routineTabAfternoon);
        TextView routineTabEvening = findViewById(R.id.routineTabEvening);
        TextView routineTabNight = findViewById(R.id.routineTabNight);

        if (routineTabAll == null) return;

        // Reset all backgrounds and text colors
        routineTabAll.setBackgroundResource(android.R.color.transparent);
        routineTabMorning.setBackgroundResource(android.R.color.transparent);
        routineTabAfternoon.setBackgroundResource(android.R.color.transparent);
        routineTabEvening.setBackgroundResource(android.R.color.transparent);
        routineTabNight.setBackgroundResource(android.R.color.transparent);

        android.util.TypedValue typedValue = new android.util.TypedValue();
        getTheme().resolveAttribute(R.attr.customTextSecondary, typedValue, true);
        int secondaryColor = typedValue.data;
        getTheme().resolveAttribute(R.attr.customTextPrimary, typedValue, true);
        int primaryColor = typedValue.data;

        routineTabAll.setTextColor(secondaryColor);
        routineTabMorning.setTextColor(secondaryColor);
        routineTabAfternoon.setTextColor(secondaryColor);
        routineTabEvening.setTextColor(secondaryColor);
        routineTabNight.setTextColor(secondaryColor);

        // Highlight selected tab
        TextView selectedTab = null;
        switch (selectedRoutineFilter) {
            case "all": selectedTab = routineTabAll; break;
            case "morning": selectedTab = routineTabMorning; break;
            case "afternoon": selectedTab = routineTabAfternoon; break;
            case "evening": selectedTab = routineTabEvening; break;
            case "night": selectedTab = routineTabNight; break;
        }

        if (selectedTab != null) {
            selectedTab.setBackgroundResource(R.drawable.glass_card_active);
            selectedTab.setTextColor(primaryColor);
        }
    }

    private void applyRoutineFilter() {
        if (currentSchedulesList == null) return;
        List<ScheduleEntity> filtered = new ArrayList<>();
        for (ScheduleEntity schedule : currentSchedulesList) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(schedule.getStartTimeMillis());
            int hour = cal.get(Calendar.HOUR_OF_DAY);

            boolean matches = false;
            switch (selectedRoutineFilter) {
                case "all":
                    matches = true;
                    break;
                case "morning":
                    matches = (hour >= 6 && hour < 12);
                    break;
                case "afternoon":
                    matches = (hour >= 12 && hour < 17);
                    break;
                case "evening":
                    matches = (hour >= 17 && hour < 20);
                    break;
                case "night":
                    matches = (hour >= 20 || hour < 6);
                    break;
            }
            if (matches) {
                filtered.add(schedule);
            }
        }

        View emptyState = findViewById(R.id.scheduleEmptyState);
        TextView emptyStateText = findViewById(R.id.scheduleEmptyStateText);

        if (filtered.isEmpty()) {
            recyclerViewSchedule.setVisibility(View.GONE);
            if (emptyState != null) {
                emptyState.setVisibility(View.VISIBLE);
                if (emptyStateText != null) {
                    switch (selectedRoutineFilter) {
                        case "all":
                            emptyStateText.setText("No schedules for this day");
                            break;
                        case "morning":
                            emptyStateText.setText("No morning schedules");
                            break;
                        case "afternoon":
                            emptyStateText.setText("No afternoon schedules");
                            break;
                        case "evening":
                            emptyStateText.setText("No evening schedules");
                            break;
                        case "night":
                            emptyStateText.setText("No night schedules");
                            break;
                    }
                }
            }
        } else {
            recyclerViewSchedule.setVisibility(View.VISIBLE);
            if (emptyState != null) emptyState.setVisibility(View.GONE);
        }

        scheduleAdapter.setItems(filtered);
    }

    private void openTaskForEditing(TaskEntity task) {
        Intent intent = new Intent(Homepage.this, Edit_task.class);
        intent.putExtra("task", task);
        startActivityForResult(intent, EDIT_TASK_REQUEST);
        overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
    }

    private void showDeleteConfirmation(TaskEntity task) {
        new MaterialAlertDialogBuilder(this, getAlertDialogTheme())
                .setTitle("🗑️ Delete Task")
                .setMessage("Are you sure you want to delete \"" + task.getName() + "\"?\n\nThis action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    taskViewModel.delete(task);
                    NotificationScheduler.cancelNotification(Homepage.this, task.getId());
                    Toast.makeText(Homepage.this, "Task deleted", Toast.LENGTH_SHORT).show();
                    refreshTaskList();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == ADD_TASK_REQUEST && data != null) {
                String title = data.getStringExtra("task_title");
                String time = data.getStringExtra("task_time");
                String date = data.getStringExtra("task_date");
                String priority = data.getStringExtra("task_priority");
                int taskId = data.getIntExtra("task_id", (int) System.currentTimeMillis());

                TaskEntity newTask = new TaskEntity();
                newTask.setId(taskId);
                newTask.setName(title);
                newTask.setTime(time);
                newTask.setDate(date);
                newTask.setPriority(priority);
                newTask.setCompleted(false);
                newTask.setCritical(priority.equalsIgnoreCase("Critical"));
                newTask.setAlarmEnabled(data.getBooleanExtra("task_alarm_enabled", true));
                newTask.setAlarmRingtoneUri(data.getStringExtra("task_ringtone_uri"));

                taskViewModel.insert(newTask);
                if (newTask.isAlarmEnabled()) {
                    scheduleTaskNotification(newTask);
                }

                Toast.makeText(this, "Task added successfully!", Toast.LENGTH_SHORT).show();

            } else if (requestCode == EDIT_TASK_REQUEST && data != null) {
                TaskEntity updatedTask = (TaskEntity) data.getSerializableExtra("updated_task");
                if (updatedTask != null) {
                    taskViewModel.update(updatedTask);

                    NotificationScheduler.cancelNotification(this, updatedTask.getId());
                    if (!updatedTask.isCompleted() && updatedTask.isAlarmEnabled()) {
                        scheduleTaskNotification(updatedTask);
                    }

                    Toast.makeText(this, "Task updated successfully!", Toast.LENGTH_SHORT).show();
                }
            }
            refreshTaskList();
        }
    }

    // ✅ Schedules notification at the task's chosen time (not immediately)
    private void scheduleTaskNotification(TaskEntity task) {
        try {
            String dateTimeStr = task.getDate() + " " + task.getTime();
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy h:mm a", Locale.getDefault());
            Date taskDate = sdf.parse(dateTimeStr);

            if (taskDate != null) {
                long triggerTime = taskDate.getTime();

                if (triggerTime >= System.currentTimeMillis()) {
                    NotificationScheduler.scheduleNotification(
                            this,
                            triggerTime,
                            task.getName(),
                            "Reminder: " + task.getName() + " at " + task.getTime(),
                            task.getPriority(),
                            task.getId(),
                            task.getAlarmRingtoneUri()
                    );
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void loadAllTasks() {
        showingCompletedTasks = false;
        taskViewModel.getAllTasks().observe(this, tasks -> {
            if (tasks != null) {
                currentTaskList = tasks;
                taskAdapter.setTasks(tasks);
                myTasksLabel.setText("All Tasks");
                myTasksLabel.setVisibility(tasks.isEmpty() ? View.GONE : View.VISIBLE);
                updateStats(tasks);
            }
        });
    }

    private void showCompletedTasks() {
        showingCompletedTasks = true;
        taskViewModel.getAllTasks().observe(this, tasks -> {
            if (tasks != null) {
                currentTaskList = tasks;
                List<TaskEntity> completedTasks = tasks.stream()
                        .filter(TaskEntity::isCompleted)
                        .collect(Collectors.toList());

                taskAdapter.setTasks(completedTasks);
                myTasksLabel.setText("Completed Tasks");
                myTasksLabel.setVisibility(completedTasks.isEmpty() ? View.GONE : View.VISIBLE);
                updateStats(tasks);
                Toast.makeText(this, "Showing completed tasks", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showIncompleteTasks() {
        showingCompletedTasks = false;
        taskViewModel.getAllTasks().observe(this, tasks -> {
            if (tasks != null) {
                currentTaskList = tasks;
                List<TaskEntity> incompleteTasks = tasks.stream()
                        .filter(t -> !t.isCompleted())
                        .collect(Collectors.toList());  // ✅ Works on API 26+

                taskAdapter.setTasks(incompleteTasks);
                myTasksLabel.setText("Incomplete Tasks");
                myTasksLabel.setVisibility(incompleteTasks.isEmpty() ? View.GONE : View.VISIBLE);
                updateStats(tasks);
                Toast.makeText(this, "Showing incomplete tasks", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateStats(List<TaskEntity> tasks) {
        if (tasks == null) return;
        String todayStr = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date());
        long todayCountVal = tasks.stream().filter(t -> todayStr.equals(t.getDate()) && !t.isCompleted()).count();
        long pendingCountVal = tasks.stream().filter(t -> !t.isCompleted()).count();
        if (todayCount != null) todayCount.setText(String.valueOf(todayCountVal));
        if (pendingCount != null) pendingCount.setText(String.valueOf(pendingCountVal));
    }

    private void refreshTaskList() {
        if (showingCompletedTasks) {
            showCompletedTasks();
        } else {
            loadAllTasks();
        }
    }

    private void setGreetingMessage(String username) {
        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        String greeting = (hour < 12) ? "Good Morning" : (hour < 18) ? "Good Afternoon" : "Good Evening";
        greetingText.setText(greeting);
        if (username != null && !username.isEmpty()) {
            textViewUsername.setText(username);
        } else {
            textViewUsername.setText("");
        }
    }

    private boolean onMenuItemClick(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_theme) {
            showThemePicker();
            return true;
        } else if (id == R.id.action_share) {
            shareContent();
            Toast.makeText(this, "Share clicked", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_sort) {
            Toast.makeText(this, "Sort By clicked", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_delete) {
            taskViewModel.deleteAll();
            cancelAllNotifications();
            Toast.makeText(this, "All tasks deleted", Toast.LENGTH_SHORT).show();
            refreshTaskList();
            return true;
        } else if (id == R.id.action_toggle_widget) {
            android.content.SharedPreferences prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
            boolean isVisible = prefs.getBoolean("show_schedule_widget", true);
            boolean newVisible = !isVisible;
            prefs.edit().putBoolean("show_schedule_widget", newVisible).apply();
            View widgetContainer = findViewById(R.id.inAppWidgetContainer);
            if (widgetContainer != null) {
                widgetContainer.setVisibility(newVisible ? View.VISIBLE : View.GONE);
            }
            Toast.makeText(this, newVisible ? "Widget shown" : "Widget hidden", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_show_completed) {
            showCompletedTasks();
            return true;
        } else if (id == R.id.action_show_incomplete) {
            showIncompleteTasks();
            return true;
        } else if (id == R.id.action_show_all) {
            loadAllTasks();
            return true;
        }
        return false;
    }

    private void cancelAllNotifications() {
        if (taskAdapter != null && taskAdapter.getCurrentList() != null) {
            for (TaskEntity task : taskAdapter.getCurrentList()) {
                NotificationScheduler.cancelNotification(this, task.getId());
            }
        }
    }

    private void shareContent() {
        String shareMessage = "Check out my Work Scheduler app! Stay on top of your tasks easily.";

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);

        // Let the user choose the app → WhatsApp, Instagram, Messages, etc.
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }

    private void showThemePicker() {
        android.view.View view = getLayoutInflater().inflate(R.layout.dialog_theme_picker, null);
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this, getAlertDialogTheme())
                .setTitle("Select Theme")
                .setView(view)
                .setNegativeButton("Cancel", null)
                .create();

        view.findViewById(R.id.btnThemeDark).setOnClickListener(v -> {
            themeManager.setTheme(ThemeManager.THEME_DARK);
            dialog.dismiss();
            recreateWithTheme();
        });
        view.findViewById(R.id.btnThemeLight).setOnClickListener(v -> {
            themeManager.setTheme(ThemeManager.THEME_LIGHT);
            dialog.dismiss();
            recreateWithTheme();
        });
        view.findViewById(R.id.btnThemeMedium).setOnClickListener(v -> {
            themeManager.setTheme(ThemeManager.THEME_MEDIUM);
            dialog.dismiss();
            recreateWithTheme();
        });

        dialog.show();
    }

    private void showProfileDialog() {
        DBHelper dbHelper = new DBHelper(this);
        String fetchedName = dbHelper.getUsername();
        final String finalUsername = (fetchedName == null || fetchedName.trim().isEmpty()) ? "User" : fetchedName;

        android.view.View view = getLayoutInflater().inflate(R.layout.dialog_profile_options, null);
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this, getAlertDialogTheme())
                .setTitle("Profile: " + finalUsername)
                .setView(view)
                .setNegativeButton("Close", null)
                .create();

        view.findViewById(R.id.btnCustomizeTheme).setOnClickListener(v -> {
            dialog.dismiss();
            showThemePicker();
        });
        view.findViewById(R.id.btnEditName).setOnClickListener(v -> {
            dialog.dismiss();
            showEditNameDialog(finalUsername);
        });

        dialog.show();
    }

    private void showEditNameDialog(String currentName) {
        android.view.View view = getLayoutInflater().inflate(R.layout.dialog_edit_name, null);
        android.widget.EditText input = view.findViewById(R.id.editNameInput);
        input.setText(currentName.equals("User") ? "" : currentName);

        new MaterialAlertDialogBuilder(this, getAlertDialogTheme())
                .setTitle("Edit Name")
                .setView(view)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        new DBHelper(this).updateUsername(newName);
                        setGreetingMessage(newName);
                        Toast.makeText(this, "Name updated", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onSaveInstanceState(@androidx.annotation.NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("active_tab_index", activeTabIndex);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }

    private class InAppWidgetAdapter extends RecyclerView.Adapter<InAppWidgetAdapter.ViewHolder> {
        private List<ScheduleEntity> items = new ArrayList<>();
        private Context context;

        public InAppWidgetAdapter(Context context) {
            this.context = context;
        }

        public void setItems(List<ScheduleEntity> items) {
            this.items = items;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.widget_item_schedule, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ScheduleEntity schedule = items.get(position);
            
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(schedule.getStartTimeMillis());
            String timeStr = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(cal.getTime());
            
            holder.time.setText(timeStr);
            holder.title.setText(schedule.getTitle());
            holder.alarm.setVisibility(schedule.isAlarmEnabled() ? View.VISIBLE : View.GONE);

            int color = android.graphics.Color.parseColor("#8CFFFFFF");
            int dotRes = R.drawable.dot_glow_medium;
            String priority = schedule.getPriority();
            if (priority != null) {
                switch (priority.toLowerCase()) {
                    case "critical": color = android.graphics.Color.parseColor("#f08080"); dotRes = R.drawable.dot_glow_critical; break;
                    case "high": color = android.graphics.Color.parseColor("#f0c060"); dotRes = R.drawable.dot_glow_high; break;
                    case "medium": color = android.graphics.Color.parseColor("#b090f0"); dotRes = R.drawable.dot_glow_medium; break;
                    case "low": color = android.graphics.Color.parseColor("#60d0c0"); dotRes = R.drawable.dot_glow_low; break;
                }
            }
            holder.dot.setImageResource(dotRes);
            holder.dot.setColorFilter(null);
            
            if (schedule.isAlarmEnabled()) {
                holder.alarm.setColorFilter(color);
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView time, title;
            ImageView dot, alarm;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                time = itemView.findViewById(R.id.widgetItemTime);
                title = itemView.findViewById(R.id.widgetItemTitle);
                dot = itemView.findViewById(R.id.widgetItemDot);
                alarm = itemView.findViewById(R.id.widgetItemAlarm);
            }
        }
    }
}
