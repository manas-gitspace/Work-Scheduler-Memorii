package com.example.workscheduler;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import java.util.ArrayList;
import java.util.List;

public class SplashOrbitalView extends View {

    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private ValueAnimator animator;
    private float time = 0f;

    static class Dot {
        float angleOffset;
        float size;
        int color;
        int alpha;
        
        Dot(float angleOffset, float size, String colorHex, float alpha) {
            this.angleOffset = angleOffset;
            this.size = size * 2.5f; // scale up slightly for high density screens
            this.color = Color.parseColor(colorHex);
            this.alpha = (int) (alpha * 255);
        }
    }

    static class Orbit {
        float radius;
        float speed;
        List<Dot> dots = new ArrayList<>();
        
        Orbit(float radius, float speed) {
            this.radius = radius * 2.5f; // scale up
            this.speed = speed;
        }
    }

    private List<Orbit> orbits = new ArrayList<>();

    public SplashOrbitalView(Context context) {
        super(context);
        init();
    }

    public SplashOrbitalView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setColor(Color.parseColor("#AEA9EC"));
        ringPaint.setAlpha((int)(0.12 * 255));
        ringPaint.setStrokeWidth(2f);

        Orbit o1 = new Orbit(50, 0.012f);
        o1.dots.add(new Dot(0f, 7f, "#9FE1CB", 0.9f));
        o1.dots.add(new Dot((float)Math.PI, 5f, "#CECBF6", 0.6f));
        orbits.add(o1);

        Orbit o2 = new Orbit(78, 0.008f);
        o2.dots.add(new Dot(1.0f, 6f, "#AFA9EC", 0.75f));
        o2.dots.add(new Dot(2.5f, 4f, "#5DCAA5", 0.55f));
        o2.dots.add(new Dot(4.8f, 5f, "#9FE1CB", 0.65f));
        orbits.add(o2);

        Orbit o3 = new Orbit(108, 0.005f);
        o3.dots.add(new Dot(0.5f, 5f, "#CECBF6", 0.55f));
        o3.dots.add(new Dot(2.2f, 4f, "#5DCAA5", 0.50f));
        o3.dots.add(new Dot(3.8f, 6f, "#AFA9EC", 0.60f));
        o3.dots.add(new Dot(5.5f, 3f, "#9FE1CB", 0.40f));
        orbits.add(o3);

        Orbit o4 = new Orbit(136, 0.003f);
        o4.dots.add(new Dot(0.8f, 4f, "#5DCAA5", 0.40f));
        o4.dots.add(new Dot(2.7f, 3f, "#CECBF6", 0.35f));
        o4.dots.add(new Dot(4.2f, 5f, "#AFA9EC", 0.45f));
        o4.dots.add(new Dot(5.8f, 3f, "#9FE1CB", 0.35f));
        orbits.add(o4);

        animator = ValueAnimator.ofFloat(0, 1000000f);
        animator.setDuration(1000000000L); // run very long
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            time += 1f; // tick
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (animator != null) animator.cancel();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float maxRadius = Math.max(w, h) * 0.65f; 
        float scale = maxRadius / 340f;
        
        if (orbits.size() >= 4) {
            orbits.get(0).radius = 125f * scale;
            orbits.get(1).radius = 195f * scale;
            orbits.get(2).radius = 270f * scale;
            orbits.get(3).radius = 340f * scale;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f - 100f; // Shifted up to match SVG center relative to screen

        // Draw rings
        for (Orbit o : orbits) {
            canvas.drawCircle(cx, cy, o.radius, ringPaint);
        }

        // Draw dots
        paint.setStyle(Paint.Style.FILL);
        for (Orbit o : orbits) {
            for (Dot d : o.dots) {
                float currentAngle = d.angleOffset + (o.speed * time);
                float dx = cx + (float) Math.cos(currentAngle) * o.radius;
                float dy = cy + (float) Math.sin(currentAngle) * o.radius;

                paint.setColor(d.color);
                paint.setAlpha(d.alpha);
                canvas.drawCircle(dx, dy, d.size, paint);
                
                // Glow effect
                paint.setAlpha((int)(d.alpha * 0.35f));
                canvas.drawCircle(dx, dy, d.size * 2.5f, paint);
            }
        }
    }
}
