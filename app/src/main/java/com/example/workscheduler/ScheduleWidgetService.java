package com.example.workscheduler;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ScheduleWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new ScheduleRemoteViewsFactory(this.getApplicationContext());
    }
}

class ScheduleRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    private Context context;
    private List<ScheduleEntity> scheduleList = new ArrayList<>();

    public ScheduleRemoteViewsFactory(Context context) {
        this.context = context;
    }

    @Override
    public void onCreate() {
        // No-op
    }

    @Override
    public void onDataSetChanged() {
        String today = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Calendar.getInstance().getTime());
        // Fetch synchronous data from Room securely
        final long identityToken = android.os.Binder.clearCallingIdentity();
        try {
            scheduleList = TaskDatabase.getDatabase(context).scheduleDao().getSchedulesForDateSync(today);
        } finally {
            android.os.Binder.restoreCallingIdentity(identityToken);
        }
    }

    @Override
    public void onDestroy() {
        scheduleList.clear();
    }

    @Override
    public int getCount() {
        return scheduleList != null ? scheduleList.size() : 0;
    }

    @Override
    public RemoteViews getViewAt(int position) {
        if (scheduleList == null || scheduleList.isEmpty()) return null;

        ScheduleEntity schedule = scheduleList.get(position);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_item_schedule);

        // Format Time
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(schedule.getStartTimeMillis());
        String timeStr = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(cal.getTime());

        views.setTextViewText(R.id.widgetItemTime, timeStr);
        views.setTextViewText(R.id.widgetItemTitle, schedule.getTitle());

        // Priority Color & Image
        int color = android.graphics.Color.parseColor("#8CFFFFFF"); // Default for alarm
        int dotRes = R.drawable.dot_glow_medium;
        String priority = schedule.getPriority();
        if (priority != null) {
            switch (priority.toLowerCase()) {
                case "critical": 
                    color = android.graphics.Color.parseColor("#f08080"); 
                    dotRes = R.drawable.dot_glow_critical;
                    break;
                case "high": 
                    color = android.graphics.Color.parseColor("#f0c060"); 
                    dotRes = R.drawable.dot_glow_high;
                    break;
                case "medium": 
                    color = android.graphics.Color.parseColor("#b090f0"); 
                    dotRes = R.drawable.dot_glow_medium;
                    break;
                case "low": 
                    color = android.graphics.Color.parseColor("#60d0c0"); 
                    dotRes = R.drawable.dot_glow_low;
                    break;
            }
        }
        views.setImageViewResource(R.id.widgetItemDot, dotRes);

        // Alarm Icon
        if (schedule.isAlarmEnabled()) {
            views.setViewVisibility(R.id.widgetItemAlarm, android.view.View.VISIBLE);
            views.setInt(R.id.widgetItemAlarm, "setColorFilter", color);
        } else {
            views.setViewVisibility(R.id.widgetItemAlarm, android.view.View.GONE);
        }

        return views;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}
