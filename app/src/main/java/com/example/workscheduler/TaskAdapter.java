package com.example.workscheduler;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TaskAdapter extends ListAdapter<TaskEntity, TaskAdapter.TaskViewHolder> {

    private List<TaskEntity> taskList;
    private Context context;
    private OnTaskCheckedChangeListener checkedChangeListener;
    private OnTaskClickListener taskClickListener;

    public interface OnTaskCheckedChangeListener {
        void onTaskCheckedChanged(TaskEntity task, boolean isChecked);
    }

    public interface OnTaskClickListener {
        void onTaskClick(TaskEntity task);
    }

    public interface OnTaskLongClickListener {
        void onTaskLongClick(TaskEntity task, int position);
    }

    private OnTaskLongClickListener taskLongClickListener;

    public void setOnTaskCheckedChangeListener(OnTaskCheckedChangeListener listener) {
        this.checkedChangeListener = listener;
    }

    public void setOnTaskClickListener(OnTaskClickListener listener) {
        this.taskClickListener = listener;
    }

    public void setOnTaskLongClickListener(OnTaskLongClickListener listener) {
        this.taskLongClickListener = listener;
    }

    public TaskAdapter(Context context) {
        super(DIFF_CALLBACK);
        this.context = context;
    }

    private static final DiffUtil.ItemCallback<TaskEntity> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<TaskEntity>() {
                @Override
                public boolean areItemsTheSame(TaskEntity oldItem, TaskEntity newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(TaskEntity oldItem, TaskEntity newItem) {
                    return oldItem.getName().equals(newItem.getName()) &&
                            oldItem.getTime().equals(newItem.getTime()) &&
                            oldItem.getPriority().equals(newItem.getPriority()) &&
                            oldItem.isCompleted() == newItem.isCompleted();
                }
            };

    @Override
    public TaskViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.task_item, parent, false);
        return new TaskViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(TaskViewHolder holder, int position) {
        // Item entrance animation
        holder.itemView.setAlpha(0f);
        holder.itemView.setTranslationY(20f);
        holder.itemView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .setStartDelay(position * 50L)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();

        TaskEntity task = getItem(position);
        holder.textViewName.setText(task.getName());
        holder.textViewPriority.setText(task.getPriority());
        holder.textViewTime.setText(task.getTime());

        // ✅ Set background color for priority label and status dot
        GradientDrawable bgDrawable = (GradientDrawable) holder.textViewPriority.getBackground();
        int dotColor;
        switch (task.getPriority().toLowerCase()) {
            case "critical":
                bgDrawable.setColor(ContextCompat.getColor(context, R.color.accent_rose_20));
                holder.textViewPriority.setTextColor(ContextCompat.getColor(context, R.color.accent_rose));
                dotColor = ContextCompat.getColor(context, R.color.accent_rose);
                break;
            case "high":
                bgDrawable.setColor(ContextCompat.getColor(context, R.color.accent_gold_20));
                holder.textViewPriority.setTextColor(ContextCompat.getColor(context, R.color.accent_gold));
                dotColor = ContextCompat.getColor(context, R.color.accent_gold);
                break;
            case "medium":
                bgDrawable.setColor(ContextCompat.getColor(context, R.color.accent_lilac_20));
                holder.textViewPriority.setTextColor(ContextCompat.getColor(context, R.color.accent_lilac));
                dotColor = ContextCompat.getColor(context, R.color.accent_lilac);
                break;
            case "low":
                bgDrawable.setColor(ContextCompat.getColor(context, R.color.accent_teal_20));
                holder.textViewPriority.setTextColor(ContextCompat.getColor(context, R.color.accent_teal));
                dotColor = ContextCompat.getColor(context, R.color.accent_teal);
                break;
            default:
                bgDrawable.setColor(ContextCompat.getColor(context, R.color.glass_bg));
                android.util.TypedValue typedValue = new android.util.TypedValue();
                context.getTheme().resolveAttribute(R.attr.customTextSecondary, typedValue, true);
                holder.textViewPriority.setTextColor(typedValue.data);
                dotColor = ContextCompat.getColor(context, R.color.text_muted);
                break;
        }
        if (holder.statusDot != null) {
            holder.statusDot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(dotColor));
        }


        // ✅ Strike-through + fade for completed tasks
        holder.checkBox.setChecked(task.isCompleted());
        if (task.isCompleted()) {
            holder.itemView.setAlpha(0.6f);
            holder.textViewName.setPaintFlags(
                    holder.textViewName.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            );
        } else {
            holder.itemView.setAlpha(1.0f);
            holder.textViewName.setPaintFlags(
                    holder.textViewName.getPaintFlags() & ~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            );
        }

        // Prevent recycled listener bugs
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(task.isCompleted());

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            TaskEntity currentTask = getItem(holder.getAdapterPosition());
            if (checkedChangeListener != null) {
                checkedChangeListener.onTaskCheckedChanged(currentTask, isChecked);
            }
        });

        // Click → open edit
        holder.itemView.setOnClickListener(v -> {
            if (taskClickListener != null) {
                taskClickListener.onTaskClick(task);
            }
        });

        // Long-click → show options
        holder.itemView.setOnLongClickListener(v -> {
            if (taskLongClickListener != null) {
                taskLongClickListener.onTaskLongClick(task, holder.getAdapterPosition());
            }
            return true;
        });
    }

    public void setTasks(List<TaskEntity> tasks) {
        this.taskList = tasks;
        submitList(tasks);
    }

    public TaskEntity getTaskAtPosition(int position) {
        List<TaskEntity> current = getCurrentList();
        if (position >= 0 && position < current.size()) {
            return current.get(position);
        }
        return null;
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        final TextView textViewName, textViewTime, textViewPriority;
        final CheckBox checkBox;
        final View statusDot;

        TaskViewHolder(View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.taskTitle);
            textViewTime = itemView.findViewById(R.id.taskTime);
            textViewPriority = itemView.findViewById(R.id.taskPriority); // ✅ matches your XML id
            checkBox = itemView.findViewById(R.id.taskCheck);
            statusDot = itemView.findViewById(R.id.taskStatusDot);
        }
    }
}
