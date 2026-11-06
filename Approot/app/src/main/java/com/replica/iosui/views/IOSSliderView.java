package com.replica.iosui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import com.replica.iosui.physics.IOSSpringAnimator;

public class IOSSliderView extends View {

    private Paint backgroundPaint;
    private Paint progressPaint;
    private RectF viewBounds;
    
    private float progress = 0.5f; // Initial standard value (0.0 to 1.0)
    private IOSSpringAnimator scaleAnimator;
    private float currentScaleX = 1.0f;
    private float currentScaleY = 1.0f;

    public IOSSliderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    private void initView() {
        // High polish hardware rendering optimization for smooth drawing on low-end chipsets
        setLayerType(LAYER_TYPE_HARDWARE, null);

        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        // iOS Translucent Liquid Glass Fill Mimic
        backgroundPaint.setColor(Color.parseColor("#4DFFFFFF")); 

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setColor(Color.WHITE);

        viewBounds = new RectF();
        scaleAnimator = new IOSSpringAnimator(this);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewBounds.set(0, 0, w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Compute perfect iOS structural round framing dynamically based on width limits
        float cornerRadius = viewBounds.width() * 0.45f;

        // Apply scale matrices for the iconic iOS pop/bounce scale effect when held down
        canvas.save();
        canvas.scale(currentScaleX, currentScaleY, viewBounds.centerX(), viewBounds.centerY());

        // Draw structural capsule backing tracking boundaries
        canvas.drawRoundRect(viewBounds, cornerRadius, cornerRadius, backgroundPaint);

        // Compute fill heights scaling upwardly from structural baseline parameters
        float fillTop = viewBounds.height() - (viewBounds.height() * progress);
        RectF progressBounds = new RectF(0, fillTop, viewBounds.width(), viewBounds.height());
        
        canvas.drawRoundRect(progressBounds, cornerRadius, cornerRadius, progressPaint);
        canvas.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Scale pop spring effect on initial click down
                scaleAnimator.animateTo(currentScaleX, 1.08f, val -> {
                    currentScaleX = val;
                    currentScaleY = val;
                    invalidate();
                });
                handleTouchPosition(event.getY());
                return true;

            case MotionEvent.ACTION_MOVE:
                handleTouchPosition(event.getY());
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // Smoothly snap back layout container limits back to nominal state frames
                scaleAnimator.animateTo(currentScaleX, 1.0f, val -> {
                    currentScaleX = val;
                    currentScaleY = val;
                    invalidate();
                });
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void handleTouchPosition(float yCoordinate) {
        float rawProgress = 1.0f - (yCoordinate / (float) getHeight());
        // Clamp variables between exact limits safely
        progress = Math.max(0.0f, Math.min(1.0f, rawProgress));
        invalidate();
    }

    public float getProgress() {
        return progress;
    }

    public void setProgress(float val) {
        this.progress = Math.max(0.0f, Math.min(1.0f, val));
        invalidate();
    }
}
