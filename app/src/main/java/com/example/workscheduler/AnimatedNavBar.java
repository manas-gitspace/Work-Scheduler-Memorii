package com.example.workscheduler;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

public class AnimatedNavBar extends LinearLayout {

    public interface OnTabSelectedListener {
        void onTabSelected(int index);
    }

    private static final int TAB_COUNT = 5;
    private final int[] iconResIds = {
            R.drawable.ic_nav_home,
            R.drawable.ic_nav_schedule,
            R.drawable.ic_nav_notes,
            R.drawable.ic_nav_diary,
            R.drawable.ic_nav_thoughts
    };
    private final String[] labels = {"Home", "Schedule", "Notes", "Diary", "Thoughts"};

    private final LinearLayout[] tabViews = new LinearLayout[TAB_COUNT];
    private final ImageView[] iconViews = new ImageView[TAB_COUNT];
    private final TextView[] labelViews = new TextView[TAB_COUNT];

    private int selectedIndex = 0;
    private OnTabSelectedListener listener;

    // Indicator paint
    private final Paint indicatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float indicatorX = 0f;
    private float indicatorTargetX = 0f;
    private final RectF indicatorRect = new RectF();

    private int accentColor;
    private int mutedColor;

    public AnimatedNavBar(Context context) {
        super(context);
        init(context);
    }

    public AnimatedNavBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public AnimatedNavBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setWeightSum(TAB_COUNT);
        setWillNotDraw(false);
        setClipChildren(false);
        setClipToPadding(false);

        int padH = dpToPx(12);
        int padV = dpToPx(10);
        setPadding(padH, padV, padH, padV);

        accentColor = ContextCompat.getColor(context, R.color.accent_lilac);
        // Resolve muted color from theme
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.customTextMuted, typedValue, true);
        mutedColor = typedValue.data;

        indicatorPaint.setColor(accentColor);
        indicatorPaint.setStyle(Paint.Style.FILL);

        glowPaint.setColor(accentColor);
        glowPaint.setAlpha(40);
        glowPaint.setStyle(Paint.Style.FILL);

        for (int i = 0; i < TAB_COUNT; i++) {
            final int index = i;

            LinearLayout tab = new LinearLayout(context);
            tab.setOrientation(VERTICAL);
            tab.setGravity(Gravity.CENTER);
            tab.setClickable(true);
            tab.setFocusable(true);

            LayoutParams tabParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
            tab.setLayoutParams(tabParams);

            // Icon
            ImageView icon = new ImageView(context);
            int iconSize = dpToPx(22);
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSize, iconSize);
            iconParams.gravity = Gravity.CENTER_HORIZONTAL;
            icon.setLayoutParams(iconParams);
            icon.setImageResource(iconResIds[i]);
            tab.addView(icon);

            // Label
            TextView label = new TextView(context);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            labelParams.gravity = Gravity.CENTER_HORIZONTAL;
            labelParams.topMargin = dpToPx(4);
            label.setLayoutParams(labelParams);
            label.setText(labels[i]);
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            label.setTypeface(label.getTypeface(), android.graphics.Typeface.BOLD);
            tab.addView(label);

            tab.setOnClickListener(v -> selectTab(index));

            tabViews[i] = tab;
            iconViews[i] = icon;
            labelViews[i] = label;
            addView(tab);
        }

        // Initial state
        post(() -> {
            updateTabAppearance(false);
            indicatorX = getTabCenterX(0);
            invalidate();
        });
    }

    public void setOnTabSelectedListener(OnTabSelectedListener listener) {
        this.listener = listener;
    }

    public void selectTab(int index) {
        if (index < 0 || index >= TAB_COUNT) return;
        if (index == selectedIndex) return;

        int oldIndex = selectedIndex;
        selectedIndex = index;

        // Animate indicator
        indicatorTargetX = getTabCenterX(index);
        ValueAnimator animator = ValueAnimator.ofFloat(indicatorX, indicatorTargetX);
        animator.setDuration(350);
        animator.setInterpolator(new OvershootInterpolator(0.8f));
        animator.addUpdateListener(anim -> {
            indicatorX = (float) anim.getAnimatedValue();
            invalidate();
        });
        animator.start();

        // Animate tabs
        animateTab(oldIndex, false);
        animateTab(index, true);

        if (listener != null) {
            listener.onTabSelected(index);
        }
    }

    private void animateTab(int index, boolean active) {
        View icon = iconViews[index];
        View label = labelViews[index];

        float targetScale = active ? 1.15f : 1.0f;
        float targetAlpha = active ? 1.0f : 0.5f;
        int targetColor = active ? accentColor : mutedColor;

        icon.animate().scaleX(targetScale).scaleY(targetScale)
                .setDuration(300)
                .setInterpolator(new OvershootInterpolator(2f))
                .start();

        label.animate().alpha(targetAlpha).setDuration(250).start();

        iconViews[index].setColorFilter(targetColor, android.graphics.PorterDuff.Mode.SRC_IN);
        labelViews[index].setTextColor(targetColor);
    }

    private void updateTabAppearance(boolean animate) {
        for (int i = 0; i < TAB_COUNT; i++) {
            boolean active = (i == selectedIndex);
            int color = active ? accentColor : mutedColor;
            float alpha = active ? 1.0f : 0.5f;
            float scale = active ? 1.15f : 1.0f;

            iconViews[i].setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
            labelViews[i].setTextColor(color);
            labelViews[i].setAlpha(alpha);
            iconViews[i].setScaleX(scale);
            iconViews[i].setScaleY(scale);
        }
    }

    private float getTabCenterX(int index) {
        if (tabViews[index].getWidth() == 0) {
            // fallback estimate
            float tabWidth = (float) getWidth() / TAB_COUNT;
            return tabWidth * index + tabWidth / 2f;
        }
        return tabViews[index].getLeft() + tabViews[index].getWidth() / 2f;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        // Draw glow behind indicator
        float pillW = dpToPx(42);
        float pillH = dpToPx(3);
        float top = dpToPx(2);

        // Outer glow
        float glowW = dpToPx(56);
        float glowH = dpToPx(8);
        indicatorRect.set(indicatorX - glowW / 2f, top - 2, indicatorX + glowW / 2f, top + glowH);
        canvas.drawRoundRect(indicatorRect, dpToPx(4), dpToPx(4), glowPaint);

        // Solid indicator pill
        indicatorRect.set(indicatorX - pillW / 2f, top, indicatorX + pillW / 2f, top + pillH);
        canvas.drawRoundRect(indicatorRect, dpToPx(2), dpToPx(2), indicatorPaint);

        super.dispatchDraw(canvas);
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }
}
