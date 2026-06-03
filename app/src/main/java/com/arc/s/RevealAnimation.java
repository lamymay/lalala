package com.arc.s;

import android.graphics.Bitmap;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * 遮挡式揭示：底图始终完整，通过黑/白遮罩块逐段移开露出图片。
 */
public enum RevealAnimation {

    CENTER_OUT_Y("🎯 中心→上下展开", Type.CENTER_OUT_Y),
    EDGES_IN_Y("🌓 上下→往中间", Type.EDGES_IN_Y),
    CENTER_OUT_X("↔️ 中心→左右展开", Type.CENTER_OUT_X),
    EDGES_IN_X("🌗 左右→往中间", Type.EDGES_IN_X),
    CENTER_OUT_XY("✨ 中心→四周展开", Type.CENTER_OUT_XY),
    EDGES_IN_XY("🌀 四周→往中间", Type.EDGES_IN_XY),
    CURTAIN_PULL("🚂 拉火车", Type.TRAIN_SCROLL);

    private enum Type {
        CENTER_OUT_Y,
        EDGES_IN_Y,
        CENTER_OUT_X,
        EDGES_IN_X,
        CENTER_OUT_XY,
        EDGES_IN_XY,
        TRAIN_SCROLL
    }

    private final String label;
    private final Type type;

    RevealAnimation(String label, Type type) {
        this.label = label;
        this.type = type;
    }

    public String getLabel() {
        return label;
    }

    public String getTileLabel() {
        int space = label.indexOf(' ');
        if (space >= 0 && space < label.length() - 1) {
            return label.substring(space + 1).trim();
        }
        return label;
    }

    /**
     * 将「每点击多露出 stepPx 像素」换算为 level 增量（0–10000）。
     * 竖向/横向动画按对应边长；四周按最短边；拉火车为进出屏各一整屏宽（共 2×宽）。
     */
    public int levelDeltaForStepPx(int stageWidth, int stageHeight, int stepPx) {
        if (stepPx <= 0) {
            stepPx = RevealStepPreferences.DEFAULT_STEP_PX;
        }
        int w = Math.max(stageWidth, 1);
        int h = Math.max(stageHeight, 1);
        float delta;
        switch (type) {
            case TRAIN_SCROLL:
                delta = stepPx * 10000f / (2f * w);
                break;
            case CENTER_OUT_Y:
            case EDGES_IN_Y:
                delta = stepPx * 10000f / h;
                break;
            case CENTER_OUT_X:
            case EDGES_IN_X:
                delta = stepPx * 10000f / w;
                break;
            case CENTER_OUT_XY:
            case EDGES_IN_XY:
                delta = stepPx * 10000f / Math.min(w, h);
                break;
            default:
                delta = stepPx * 10000f / h;
                break;
        }
        int levelDelta = Math.round(delta);
        return Math.max(1, Math.min(levelDelta, 5000));
    }

    public void apply(RevealViews views, Bitmap bitmap, int level, int coverColor) {
        views.imageView.setImageBitmap(bitmap);
        views.imageView.setAlpha(1f);

        if (level <= 0 && type == Type.TRAIN_SCROLL) {
            prepareTrainIdle(views);
            return;
        }

        resetImage(views.imageView);

        if (level <= 0) {
            views.showFullBlackCover();
            return;
        }

        views.imageView.setVisibility(View.VISIBLE);
        views.hideFullCover();
        views.setCoverColor(coverColor);

        float progress = level / 10000f;
        int w = views.stageWidth();
        int h = views.stageHeight();
        views.hidePartialMasks();

        switch (type) {
            case CENTER_OUT_Y:
                applyCenterOutY(views, progress, w, h);
                break;
            case EDGES_IN_Y:
                applyEdgesInY(views, progress, w, h);
                break;
            case CENTER_OUT_X:
                applyCenterOutX(views, progress, w, h);
                break;
            case EDGES_IN_X:
                applyEdgesInX(views, progress, w, h);
                break;
            case CENTER_OUT_XY:
                applyCenterOutXy(views, progress, w, h);
                break;
            case EDGES_IN_XY:
                applyEdgesInXy(views, progress, w, h);
                break;
            case TRAIN_SCROLL:
                applyTrainScroll(views, progress, w, h);
                break;
            default:
                break;
        }
    }

    /** 从中心向上下：上下两块遮罩从中线往外侧退去。 */
    private static void applyCenterOutY(RevealViews views, float progress, int w, int h) {
        int band = Math.round((1f - progress) * h / 2f);
        layoutMask(views.maskTop, w, band, Gravity.TOP);
        layoutMask(views.maskBottom, w, band, Gravity.BOTTOM);
    }

    /** 从上/下边往中间：中间一块遮罩逐渐缩小。 */
    private static void applyEdgesInY(RevealViews views, float progress, int w, int h) {
        int band = Math.round((1f - progress) * h);
        layoutMask(views.maskCenter, w, band, Gravity.CENTER);
    }

    private static void applyCenterOutX(RevealViews views, float progress, int w, int h) {
        int band = Math.round((1f - progress) * w / 2f);
        layoutMask(views.maskLeft, band, h, Gravity.START);
        layoutMask(views.maskRight, band, h, Gravity.END);
    }

    private static void applyEdgesInX(RevealViews views, float progress, int w, int h) {
        int band = Math.round((1f - progress) * w);
        layoutMask(views.maskCenter, band, h, Gravity.CENTER);
    }

    private static void applyCenterOutXy(RevealViews views, float progress, int w, int h) {
        int bandY = Math.round((1f - progress) * h / 2f);
        int bandX = Math.round((1f - progress) * w / 2f);
        layoutMask(views.maskTop, w, bandY, Gravity.TOP);
        layoutMask(views.maskBottom, w, bandY, Gravity.BOTTOM);
        layoutMask(views.maskLeft, bandX, h, Gravity.START);
        layoutMask(views.maskRight, bandX, h, Gravity.END);
    }

    private static void applyEdgesInXy(RevealViews views, float progress, int w, int h) {
        int mw = Math.round((1f - progress) * w);
        int mh = Math.round((1f - progress) * h);
        layoutMask(views.maskCenter, mw, mh, Gravity.CENTER);
    }

    /** 拉火车待机：纯黑屏，图片在屏外待命（不绘制、不占布局）。 */
    private static void prepareTrainIdle(RevealViews views) {
        int w = views.stageWidth();
        views.hidePartialMasks();
        views.imageView.setScaleX(1f);
        views.imageView.setScaleY(1f);
        views.imageView.setRotation(0f);
        views.imageView.setClipBounds(null);
        views.imageView.setTranslationX(-w);
        views.imageView.setTranslationY(0f);
        views.imageView.setVisibility(View.GONE);
        views.showFullBlackCover();
    }

    /**
     * 拉火车：level=0 仅黑屏；首次点击后图从屏外左侧进入（右缘贴左边界），
     * 每次再向右 stepPx，直至左缘离开屏右缘后循环。
     * progress 0 → -w，progress 1 → +w。
     */
    private static void applyTrainScroll(RevealViews views, float progress, int w, int h) {
        views.hidePartialMasks();
        float offsetX = -w + progress * 2f * w;
        views.imageView.setTranslationX(offsetX);
        views.imageView.setTranslationY(0f);
    }

    private static void layoutMask(View mask, int width, int height, int gravity) {
        if (height <= 0 && width <= 0) {
            mask.setVisibility(View.GONE);
            return;
        }
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                Math.max(width, 0),
                Math.max(height, 0),
                gravity
        );
        mask.setLayoutParams(lp);
        mask.setVisibility(View.VISIBLE);
        mask.setTranslationX(0f);
        mask.setTranslationY(0f);
    }

    private static void resetImage(ImageView imageView) {
        imageView.setVisibility(View.VISIBLE);
        imageView.setScaleX(1f);
        imageView.setScaleY(1f);
        imageView.setTranslationX(0f);
        imageView.setTranslationY(0f);
        imageView.setRotation(0f);
        imageView.setClipBounds(null);
    }
}
