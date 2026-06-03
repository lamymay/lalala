package com.arc.s;

import android.graphics.Color;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * 图片层 + 遮挡层（不参与缩放，只改遮罩区域）。
 */
final class RevealViews {

    static final int INITIAL_COVER_COLOR = Color.BLACK;

    final FrameLayout stage;
    final ImageView imageView;
    final View maskTop;
    final View maskBottom;
    final View maskLeft;
    final View maskRight;
    final View maskCenter;
    final View maskPan;
    final View maskFull;

    RevealViews(
            FrameLayout stage,
            ImageView imageView,
            View maskTop,
            View maskBottom,
            View maskLeft,
            View maskRight,
            View maskCenter,
            View maskPan,
            View maskFull
    ) {
        this.stage = stage;
        this.imageView = imageView;
        this.maskTop = maskTop;
        this.maskBottom = maskBottom;
        this.maskLeft = maskLeft;
        this.maskRight = maskRight;
        this.maskCenter = maskCenter;
        this.maskPan = maskPan;
        this.maskFull = maskFull;
    }

    int stageWidth() {
        if (stage.getWidth() > 0) {
            return stage.getWidth();
        }
        return stage.getResources().getDisplayMetrics().widthPixels;
    }

    int stageHeight() {
        if (stage.getHeight() > 0) {
            return stage.getHeight();
        }
        return stage.getResources().getDisplayMetrics().heightPixels;
    }

    void setCoverColor(int color) {
        stage.setBackgroundColor(INITIAL_COVER_COLOR);
        maskTop.setBackgroundColor(color);
        maskBottom.setBackgroundColor(color);
        maskLeft.setBackgroundColor(color);
        maskRight.setBackgroundColor(color);
        maskCenter.setBackgroundColor(color);
        maskPan.setBackgroundColor(color);
    }

    void showFullBlackCover() {
        hidePartialMasks();
        maskFull.setVisibility(View.VISIBLE);
        maskFull.setBackgroundColor(INITIAL_COVER_COLOR);
        maskFull.bringToFront();
        stage.setBackgroundColor(INITIAL_COVER_COLOR);
    }

    void hideFullCover() {
        maskFull.setVisibility(View.GONE);
    }

    void hidePartialMasks() {
        maskTop.setVisibility(View.GONE);
        maskBottom.setVisibility(View.GONE);
        maskLeft.setVisibility(View.GONE);
        maskRight.setVisibility(View.GONE);
        maskCenter.setVisibility(View.GONE);
        maskPan.setVisibility(View.GONE);
        maskPan.setTranslationX(0f);
    }
}
