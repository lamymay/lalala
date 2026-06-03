package com.arc.s;

import android.graphics.Bitmap;
import android.graphics.Color;

/**
 * 根据图片整体明暗选择遮挡色：偏暗用白遮罩，偏亮用黑遮罩。
 */
final class CoverColorUtil {

    private static final int SAMPLE_SIZE = 32;

    private CoverColorUtil() {
    }

    static int pickCoverColor(Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) {
            return Color.BLACK;
        }

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width <= 0 || height <= 0) {
            return Color.BLACK;
        }

        Bitmap sample = Bitmap.createScaledBitmap(bitmap, SAMPLE_SIZE, SAMPLE_SIZE, true);
        long sum = 0;
        int count = SAMPLE_SIZE * SAMPLE_SIZE;
        for (int y = 0; y < SAMPLE_SIZE; y++) {
            for (int x = 0; x < SAMPLE_SIZE; x++) {
                int pixel = sample.getPixel(x, y);
                int r = Color.red(pixel);
                int g = Color.green(pixel);
                int b = Color.blue(pixel);
                sum += (r * 299 + g * 587 + b * 114) / 1000;
            }
        }
        if (sample != bitmap) {
            sample.recycle();
        }

        float average = sum / (float) count;
        return average < 128f ? Color.WHITE : Color.BLACK;
    }
}
