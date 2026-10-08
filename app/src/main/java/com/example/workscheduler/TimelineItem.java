package com.example.workscheduler;

public class TimelineItem {
    public int hour; // 0 to 23
    public String timeLabel; // "08:00"
    public boolean isEmpty;
    public ScheduleEntity schedule;

    public TimelineItem(int hour, String timeLabel, boolean isEmpty, ScheduleEntity schedule) {
        this.hour = hour;
        this.timeLabel = timeLabel;
        this.isEmpty = isEmpty;
        this.schedule = schedule;
    }
}
