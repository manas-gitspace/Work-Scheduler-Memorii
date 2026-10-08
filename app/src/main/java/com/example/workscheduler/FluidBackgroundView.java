package com.example.workscheduler;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class FluidBackgroundView extends View {
    private Paint paint;
    private float animatedValue = 0f;
    private ValueAnimator animator;

    public FluidBackgroundView(Context context) {
        super(context);
        init();
    }

    public FluidBackgroundView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        animator = ValueAnimator.ofFloat(0f, (float) (Math.PI * 2));
        animator.setDuration(15000); // 15 seconds for a full cycle
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            animatedValue = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();

        if (w == 0 || h == 0) return;

        // Shifting coordinates for fluid movement
        float x0 = w * 0.5f + (float) Math.sin(animatedValue) * w * 0.5f;
        float y0 = h * 0.2f + (float) Math.cos(animatedValue * 0.7f) * h * 0.2f;
        float x1 = w * 0.5f + (float) Math.sin(animatedValue + Math.PI) * w * 0.5f;
        float y1 = h * 0.8f + (float) Math.cos(animatedValue * 1.3f) * h * 0.2f;

        // Dark deep space fluid colors
        int color1 = Color.parseColor("#1a0b2e");
        int color2 = Color.parseColor("#0f1123");
        int color3 = Color.parseColor("#0a2a29");
        int color4 = Color.parseColor("#131a38");

        LinearGradient gradient = new LinearGradient(
                x0, y0, x1, y1,
                new int[]{color1, color2, color3, color4},
                new float[]{0f, 0.3f, 0.7f, 1f},
                Shader.TileMode.MIRROR
        );

        paint.setShader(gradient);
        canvas.drawRect(0, 0, w, h, paint);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (animator != null) animator.cancel();
    }
}
