package com.example.workscheduler;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "schedules")
public class ScheduleEntity implements Serializable {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String title;
    private String date; // Format: "dd MMM yyyy"
    
    private long startTimeMillis;
    private long endTimeMillis;
    private boolean isFullDay;

    private boolean alarmEnabled;
    private String priority; // To determine accent color: "Critical", "High", "Medium", "Low"

    public ScheduleEntity() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public long getStartTimeMillis() { return startTimeMillis; }
    public void setStartTimeMillis(long startTimeMillis) { this.startTimeMillis = startTimeMillis; }

    public long getEndTimeMillis() { return endTimeMillis; }
    public void setEndTimeMillis(long endTimeMillis) { this.endTimeMillis = endTimeMillis; }

    public boolean isFullDay() { return isFullDay; }
    public void setFullDay(boolean fullDay) { isFullDay = fullDay; }

    public boolean isAlarmEnabled() { return alarmEnabled; }
    public void setAlarmEnabled(boolean alarmEnabled) { this.alarmEnabled = alarmEnabled; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
}
