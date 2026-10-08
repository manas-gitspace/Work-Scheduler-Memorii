package com.example.workscheduler;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ScheduleWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, R.layout.widget_schedule);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, int layoutId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), layoutId);

        // Set up the intent that starts the ScheduleWidgetService, which will provide the views for this collection.
        Intent intent = new Intent(context, ScheduleWidgetService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        
        views.setRemoteAdapter(R.id.widgetListView, intent);
        views.setEmptyView(R.id.widgetListView, R.id.widgetEmptyText);

        // Intent to launch app when clicking title
        Intent mainIntent = new Intent(context, SplashActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widgetTitle, pendingIntent);

        // Update the item count asynchronously
        TaskDatabase.databaseWriteExecutor.execute(() -> {
            String today = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Calendar.getInstance().getTime());
            int count = TaskDatabase.getDatabase(context).scheduleDao().getSchedulesForDateSync(today).size();
            views.setTextViewText(R.id.widgetItemCount, count + (count == 1 ? " item" : " items"));
            appWidgetManager.updateAppWidget(appWidgetId, views);
        });

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
    
    public static void sendRefreshBroadcast(Context context) {
        Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.setComponent(new android.content.ComponentName(context, ScheduleWidgetProvider.class));
        context.sendBroadcast(intent);
    }
}
