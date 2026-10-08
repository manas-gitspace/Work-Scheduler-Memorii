package com.example.workscheduler;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.DayViewHolder> {

    private List<CalendarDay> days = new ArrayList<>();
    private int selectedPosition = -1;
    private OnDateSelectedListener listener;
    private OnDateLongClickListener longClickListener;
    private Set<String> datesWithEvents = new HashSet<>();

    public interface OnDateSelectedListener {
        void onDateSelected(Calendar date);
    }

    public interface OnDateLongClickListener {
        void onDateLongClick(Calendar date);
    }

    public CalendarAdapter(OnDateSelectedListener listener) {
        this.listener = listener;
    }

    public void setOnDateLongClickListener(OnDateLongClickListener listener) {
        this.longClickListener = listener;
    }

    public boolean hasEvents(Calendar date) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        String dateStr = sdf.format(date.getTime());
        return datesWithEvents.contains(dateStr);
    }

    public void setDays(List<CalendarDay> days, int todayPosition) {
        this.days = days;
        this.selectedPosition = todayPosition;
        notifyDataSetChanged();
    }

    public void setSelectedPosition(int position) {
        int old = selectedPosition;
        selectedPosition = position;
        if (old >= 0 && old < days.size()) notifyItemChanged(old);
        if (position >= 0 && position < days.size()) notifyItemChanged(position);
    }

    public void setDatesWithEvents(Set<String> dates) {
        this.datesWithEvents = dates;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_day, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        CalendarDay day = days.get(position);
        Context context = holder.itemView.getContext();

        holder.textDayNumber.setText(String.valueOf(day.dayOfMonth));

        boolean isSelected = (position == selectedPosition);
        boolean isToday = day.isToday;
        boolean isCurrentMonth = day.isCurrentMonth;

        // Reset
        holder.dayBackground.setVisibility(View.GONE);
        holder.textDayNumber.setTextColor(resolveThemeAttr(context, R.attr.customTextPrimary));

        if (!isCurrentMonth) {
            holder.textDayNumber.setTextColor(resolveThemeAttr(context, R.attr.customTextMuted));
        }

        if (isSelected) {
            holder.dayBackground.setVisibility(View.VISIBLE);
            holder.dayBackground.setBackgroundResource(R.drawable.bg_calendar_selected);
            holder.textDayNumber.setTextColor(Color.WHITE);
        } else if (isToday) {
            holder.dayBackground.setVisibility(View.VISIBLE);
            holder.dayBackground.setBackgroundResource(R.drawable.bg_calendar_today);
        }

        // Event dot
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        String dateStr = sdf.format(day.calendar.getTime());
        if (datesWithEvents.contains(dateStr) && !isSelected) {
            holder.eventDot.setVisibility(View.VISIBLE);
        } else {
            holder.eventDot.setVisibility(View.INVISIBLE);
        }

        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                setSelectedPosition(pos);
                if (listener != null) listener.onDateSelected(days.get(pos).calendar);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && longClickListener != null) {
                longClickListener.onDateLongClick(days.get(pos).calendar);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    private int resolveThemeAttr(Context context, int attr) {
        android.util.TypedValue typedValue = new android.util.TypedValue();
        context.getTheme().resolveAttribute(attr, typedValue, true);
        return context.getResources().getColor(typedValue.resourceId, context.getTheme());
    }

    public static class DayViewHolder extends RecyclerView.ViewHolder {
        TextView textDayNumber;
        View dayBackground;
        View eventDot;

        public DayViewHolder(@NonNull View itemView) {
            super(itemView);
            textDayNumber = itemView.findViewById(R.id.textDayNumber);
            dayBackground = itemView.findViewById(R.id.dayBackground);
            eventDot = itemView.findViewById(R.id.eventDot);
        }
    }

    public static class CalendarDay {
        public int dayOfMonth;
        public boolean isCurrentMonth;
        public boolean isToday;
        public Calendar calendar;

        public CalendarDay(int dayOfMonth, boolean isCurrentMonth, boolean isToday, Calendar calendar) {
            this.dayOfMonth = dayOfMonth;
            this.isCurrentMonth = isCurrentMonth;
            this.isToday = isToday;
            this.calendar = calendar;
        }
    }

    public static List<CalendarDay> generateDaysForMonth(Calendar month) {
        List<CalendarDay> days = new ArrayList<>();
        Calendar cal = (Calendar) month.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);

        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1; // Sunday = 0
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        Calendar today = Calendar.getInstance();

        // Previous month overflow
        Calendar prev = (Calendar) cal.clone();
        prev.add(Calendar.MONTH, -1);
        int daysInPrevMonth = prev.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int i = firstDayOfWeek - 1; i >= 0; i--) {
            Calendar dayCal = (Calendar) prev.clone();
            int day = daysInPrevMonth - i;
            dayCal.set(Calendar.DAY_OF_MONTH, day);
            days.add(new CalendarDay(day, false, false, dayCal));
        }

        // Current month
        for (int i = 1; i <= daysInMonth; i++) {
            Calendar dayCal = (Calendar) cal.clone();
            dayCal.set(Calendar.DAY_OF_MONTH, i);
            boolean isToday = (dayCal.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                    && dayCal.get(Calendar.MONTH) == today.get(Calendar.MONTH)
                    && dayCal.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH));
            days.add(new CalendarDay(i, true, isToday, dayCal));
        }

        // Next month overflow to fill 6 rows (42 cells)
        int remaining = 42 - days.size();
        Calendar next = (Calendar) cal.clone();
        next.add(Calendar.MONTH, 1);
        for (int i = 1; i <= remaining; i++) {
            Calendar dayCal = (Calendar) next.clone();
            dayCal.set(Calendar.DAY_OF_MONTH, i);
            days.add(new CalendarDay(i, false, false, dayCal));
        }

        return days;
    }

    public static int findTodayPosition(List<CalendarDay> days) {
        for (int i = 0; i < days.size(); i++) {
            if (days.get(i).isToday) return i;
        }
        return -1;
    }
}
