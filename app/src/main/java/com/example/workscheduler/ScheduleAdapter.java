package com.example.workscheduler;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ViewHolder> {

    private List<ScheduleEntity> items = new ArrayList<>();
    private final Context context;

    public interface OnScheduleClickListener {
        void onScheduleClicked(ScheduleEntity schedule);
    }

    private OnScheduleClickListener listener;

    public ScheduleAdapter(Context context) {
        this.context = context;
    }

    public void setListener(OnScheduleClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<ScheduleEntity> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    public ScheduleEntity getItem(int position) {
        if (position >= 0 && position < items.size()) {
            return items.get(position);
        }
        return null;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_schedule_timeline, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScheduleEntity schedule = items.get(position);
        boolean isLast = (position == items.size() - 1);

        holder.textTitle.setText(schedule.getTitle());

        // Format time for left label
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(schedule.getStartTimeMillis());
        String startTime = sdf.format(cal.getTime());
        holder.textTimeLabel.setText(startTime);

        // Duration subtitle
        if (schedule.getEndTimeMillis() > 0) {
            long durationMin = (schedule.getEndTimeMillis() - schedule.getStartTimeMillis()) / 60000;
            if (durationMin > 0) {
                holder.textSubtitle.setText(durationMin + " min");
                holder.textSubtitle.setVisibility(View.VISIBLE);
            } else {
                holder.textSubtitle.setVisibility(View.GONE);
            }
        } else {
            holder.textSubtitle.setVisibility(View.GONE);
        }

        // Alarm icon
        holder.iconAlarm.setVisibility(schedule.isAlarmEnabled() ? View.VISIBLE : View.GONE);

        // Determine time-of-day color
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int dotColor;
        String categoryLabel;
        if (hour >= 6 && hour < 12) {
            dotColor = Color.parseColor("#F0A050"); // Warm orange for morning
            categoryLabel = "MORNING";
        } else if (hour >= 12 && hour < 17) {
            dotColor = Color.parseColor("#60d0c0"); // Teal for afternoon
            categoryLabel = "AFTERNOON";
        } else if (hour >= 17 && hour < 20) {
            dotColor = Color.parseColor("#f08080"); // Rose for evening
            categoryLabel = "EVENING";
        } else {
            dotColor = Color.parseColor("#b090f0"); // Lilac for night
            categoryLabel = "NIGHT";
        }

        // Override category label with priority if set
        String priority = schedule.getPriority();
        if (priority != null && !priority.isEmpty()) {
            categoryLabel = priority.toUpperCase();
            switch (priority.toLowerCase()) {
                case "critical": dotColor = Color.parseColor("#E06060"); break;
                case "high": dotColor = Color.parseColor("#F0A050"); break;
                case "medium": dotColor = Color.parseColor("#60d0c0"); break;
                case "low": dotColor = Color.parseColor("#b090f0"); break;
            }
        }

        // Set dot colors
        GradientDrawable dotBg = (GradientDrawable) holder.timelineDot.getBackground().mutate();
        dotBg.setColor(dotColor);

        GradientDrawable categoryDotBg = (GradientDrawable) holder.categoryDot.getBackground().mutate();
        categoryDotBg.setColor(dotColor);

        // Set category label
        holder.textCategory.setText(categoryLabel);
        holder.textCategory.setTextColor(dotColor);

        // Timeline line — hide for last item
        holder.timelineLine.setVisibility(isLast ? View.INVISIBLE : View.VISIBLE);

        // Card accent — subtle tint of the card background based on dot color
        int tintColor = Color.argb(15, Color.red(dotColor), Color.green(dotColor), Color.blue(dotColor));
        holder.scheduleCardRoot.setBackgroundResource(R.drawable.bg_schedule_card);
        holder.scheduleCardRoot.setBackgroundTintList(ColorStateList.valueOf(tintColor));

        // Entry animation
        holder.itemView.setAlpha(0f);
        holder.itemView.setTranslationY(20f);
        holder.itemView.animate().alpha(1f).translationY(0f).setDuration(350)
                .setStartDelay(Math.min(position * 60L, 300))
                .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onScheduleClicked(schedule);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle, textTimeLabel, textSubtitle, textCategory;
        View timelineDot, timelineLine, categoryDot, scheduleCardRoot;
        ImageView iconAlarm;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.textTitle);
            textTimeLabel = itemView.findViewById(R.id.textTimeLabel);
            textSubtitle = itemView.findViewById(R.id.textSubtitle);
            textCategory = itemView.findViewById(R.id.textCategory);
            timelineDot = itemView.findViewById(R.id.timelineDot);
            timelineLine = itemView.findViewById(R.id.timelineLine);
            categoryDot = itemView.findViewById(R.id.categoryDot);
            scheduleCardRoot = itemView.findViewById(R.id.scheduleCardRoot);
            iconAlarm = itemView.findViewById(R.id.iconAlarm);
        }
    }
}
