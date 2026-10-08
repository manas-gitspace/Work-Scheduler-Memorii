package com.example.workscheduler;

import android.appwidget.AppWidgetManager;
import android.content.Context;

public class ScheduleWidgetProvider1x2 extends ScheduleWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, R.layout.widget_schedule_1x2);
        }
    }
}
