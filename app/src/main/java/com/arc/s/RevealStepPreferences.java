package com.arc.s;

import android.content.Context;
import android.content.SharedPreferences;

final class RevealStepPreferences {

    static final int[] STEP_OPTIONS_PX = {16, 24, 32, 48, 64, 80, 96, 128, 160, 192, 256};
    static final int DEFAULT_STEP_PX = 48;
    static final int MIN_STEP_PX = 1;
    static final int MAX_STEP_PX = 2000;

    private static final String PREF_NAME = "lalala_prefs";
    private static final String KEY_REVEAL_STEP_PX = "reveal_step_px";

    private RevealStepPreferences() {
    }

    static int getRevealStepPx(Context context) {
        int step = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_REVEAL_STEP_PX, DEFAULT_STEP_PX);
        return isValidStep(step) ? step : DEFAULT_STEP_PX;
    }

    static void setRevealStepPx(Context context, int stepPx) {
        if (!isValidStep(stepPx)) {
            return;
        }
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_REVEAL_STEP_PX, stepPx)
                .apply();
    }

    static boolean isValidStep(int step) {
        return step >= MIN_STEP_PX && step <= MAX_STEP_PX;
    }

    static int indexOfPreset(int stepPx) {
        for (int i = 0; i < STEP_OPTIONS_PX.length; i++) {
            if (STEP_OPTIONS_PX[i] == stepPx) {
                return i;
            }
        }
        return -1;
    }
}
