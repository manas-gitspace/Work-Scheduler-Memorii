package com.example.workscheduler;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import androidx.room.Ignore;

import java.io.Serializable;

@Entity(tableName = "tasks")
public class TaskEntity implements Serializable {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String name;
    private String time;
    private String date;
    private String priority;

    // ✅ NEW: completed flag
    private boolean isCompleted;
    private boolean critical;

    // ✅ NEW: alarm with ringtone
    private boolean alarmEnabled;
    private String alarmRingtoneUri;

    public TaskEntity() {}

    @Ignore
    public TaskEntity(String name, String time, String date, String priority) {
        this.name = name;
        this.time = time;
        this.date = date;
        this.priority = priority;
        this.isCompleted = false;
        this.alarmEnabled = true; // default to true since old tasks had alarms
    }

    // Getters / setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; } // ← fixed name

    // ✅ NEW: completed getter/setter
    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { this.isCompleted = completed; }
    public boolean isCritical() { return critical; }
    public void setCritical(boolean critical) { this.critical = critical; }

    public boolean isAlarmEnabled() { return alarmEnabled; }
    public void setAlarmEnabled(boolean alarmEnabled) { this.alarmEnabled = alarmEnabled; }

    public String getAlarmRingtoneUri() { return alarmRingtoneUri; }
    public void setAlarmRingtoneUri(String alarmRingtoneUri) { this.alarmRingtoneUri = alarmRingtoneUri; }
}
