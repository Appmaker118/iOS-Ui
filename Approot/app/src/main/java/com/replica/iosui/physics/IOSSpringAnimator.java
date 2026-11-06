package com.replica.iosui.physics;

import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class IOSSpringAnimator {

    // iOS Default Core Tuning Constants
    private float mass = 1.0f;
    private float stiffness = 300.0f;  // High stiffness for crisp, snappy response
    private float damping = 28.0f;     // Calculated damping ratio for perfect bounce without endless jitter
    
    private final View targetView;
    private ValueAnimator frameAnimator;

    public interface SpringUpdateListener {
        void onUpdate(float value);
    }

    public IOSSpringAnimator(View targetView) {
        this.targetView = targetView;
    }

    public void animateTo(final float startValue, final float targetValue, final SpringUpdateListener listener) {
        if (frameAnimator != null) {
            frameAnimator.cancel();
        }

        // Apple style frame loop driver
        frameAnimator = ValueAnimator.ofFloat(0f, 1f);
        frameAnimator.setDuration(600); // Max fallback duration
        frameAnimator.setInterpolator(new LinearInterpolator());

        frameAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            private float lastTime = 0;
            private float currentPosition = startValue;
            private float currentVelocity = 0;

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float totalProgress = animation.getAnimatedFraction();
                if (totalProgress == 0) {
                    lastTime = 0;
                    return;
                }

                float currentTime = totalProgress * 0.6f; // Scale to seconds
                float dt = currentTime - lastTime;
                if (dt <= 0) dt = 0.016f; // Fallback to ~60fps step if timing drops

                lastTime = currentTime;

                // Core Hooke's Law Spring Physics Calculation: F = -kX - cV
                float displacement = currentPosition - targetValue;
                float springForce = -stiffness * displacement;
                float dampingForce = -damping * currentVelocity;
                float acceleration = (springForce + dampingForce) / mass;

                currentVelocity += acceleration * dt;
                currentPosition += currentVelocity * dt;

                // Push calculated coordinates directly into the custom UI interface listener
                listener.onUpdate(currentPosition);

                // Early exit threshold once spring micro-oscillations stop completely
                if (Math.abs(currentPosition - targetValue) < 0.001f && Math.abs(currentVelocity) < 0.001f) {
                    frameAnimator.cancel();
                    listener.onUpdate(targetValue);
                }
            }
        });

        frameAnimator.start();
    }
}
