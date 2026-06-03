package com.arc.s;

import android.view.animation.Interpolator;

/**
 * 叶片飘落：前段重力下落，落地时略过冲再回弹到静止（返回值可略大于 1）。
 */
final class PathLeafFallInterpolator implements Interpolator {

    private static final float FALL_PHASE = 0.78f;
    private static final float BOUNCE_PEAK = 1.1f;

    @Override
    public float getInterpolation(float input) {
        if (input <= FALL_PHASE) {
            float t = input / FALL_PHASE;
            return t * t * t * (BOUNCE_PEAK - 0.12f);
        }
        float t = (input - FALL_PHASE) / (1f - FALL_PHASE);
        float wave = (float) Math.sin(t * Math.PI);
        return (BOUNCE_PEAK - 0.12f) + wave * 0.12f + t * (1f - BOUNCE_PEAK + 0.12f);
    }
}
