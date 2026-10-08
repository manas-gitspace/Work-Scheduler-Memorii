package com.example.workscheduler;

import java.io.Serializable;

public class TaskModel implements Serializable {
    private String name;
    private String time;
    private String priority;
    private boolean completed;

    public TaskModel(String name, String time, String priority) {
        this.name = name;
        this.time = time;
        this.priority = priority;
        this.completed = false;
    }

    // Getters
    public String getName() { return name; }
    public String getTime() { return time; }
    public String getPriority() { return priority; }
    public boolean isCompleted() { return completed; }
    public boolean isCritical() { return "Critical".equals(priority); }

    // Setters
    public void setName(String name) { this.name = name; }
    public void setTime(String time) { this.time = time; }
    public void setPriority(String priority) { this.priority = priority; }
    public void setCompleted(boolean completed) { this.completed = completed; }
}