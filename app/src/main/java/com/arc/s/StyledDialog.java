package com.arc.s;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * 菜单叠在 Activity 之上：背后仍是当前游戏画面（冻结），无灰色蒙层。
 */
final class StyledDialog {

    private static final float POPUP_START_SCALE = 0.001f;
    private static final float POPUP_OVERSHOOT = 3.2f;
    private static final long POPUP_ENTER_MS = 460L;
    private static final long POPUP_EXIT_MS = 260L;

    private static final long LEAF_ENTER_MS = 680L;
    private static final long LEAF_EXIT_MS = 420L;
    private static final float LEAF_START_ROTATION = -16f;
    private static final float LEAF_SWAY_ROTATION = 10f;
    private static final int LEAF_SWAY_X_DP = 10;

    interface OnDismissListener {
        void onDismiss();
    }

    private final View overlayRoot;
    private final View panel;
    private final View scrim;
    private final View content;
    private final DialogAnimationStyle style;
    @Nullable
    private OnBackPressedCallback backCallback;
    private ValueAnimator runningAnimator;

    private StyledDialog(
            View overlayRoot,
            View panel,
            View scrim,
            View content,
            DialogAnimationStyle style
    ) {
        this.overlayRoot = overlayRoot;
        this.panel = panel;
        this.scrim = scrim;
        this.content = content;
        this.style = style;
    }

    static StyledDialog show(
            @NonNull AppCompatActivity activity,
            @NonNull View content,
            @NonNull DialogAnimationStyle style
    ) {
        return show(activity, content, style, null);
    }

    static StyledDialog show(
            @NonNull AppCompatActivity activity,
            @NonNull View content,
            @NonNull DialogAnimationStyle style,
            OnDismissListener onDismissListener
    ) {
        ViewGroup host = activity.findViewById(R.id.root);
        View overlayRoot = activity.getLayoutInflater()
                .inflate(R.layout.dialog_overlay, host, false);
        FrameLayout panel = overlayRoot.findViewById(R.id.dialog_panel);
        View scrim = overlayRoot.findViewById(R.id.scrim);

        FrameLayout.LayoutParams panelLp = (FrameLayout.LayoutParams) panel.getLayoutParams();
        if (style == DialogAnimationStyle.DROP_FROM_TOP) {
            panelLp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            panelLp.topMargin = getStatusBarHeight(activity) + dp(activity, 8);
            panelLp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            panelLp.leftMargin = dp(activity, 12);
            panelLp.rightMargin = dp(activity, 12);
        } else {
            panelLp.gravity = Gravity.CENTER;
            panelLp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            panelLp.leftMargin = dp(activity, 32);
            panelLp.rightMargin = dp(activity, 32);
        }
        panel.setLayoutParams(panelLp);
        panel.removeAllViews();
        panel.addView(content);
        if (style == DialogAnimationStyle.DROP_FROM_TOP) {
            content.setBackgroundResource(R.drawable.bg_dialog_panel_drop);
        } else {
            content.setBackgroundResource(R.drawable.bg_dialog_panel);
        }

        if (style == DialogAnimationStyle.DROP_FROM_TOP) {
            applyPathLeafOffscreenStart(panel, activity);
        }

        host.addView(overlayRoot, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        StyledDialog styledDialog = new StyledDialog(overlayRoot, panel, scrim, content, style);
        scrim.setOnClickListener(v -> styledDialog.dismiss(onDismissListener));

        OnBackPressedCallback backCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                styledDialog.dismiss(onDismissListener);
            }
        };
        activity.getOnBackPressedDispatcher().addCallback(activity, backCallback);
        styledDialog.backCallback = backCallback;

        styledDialog.playEnterAnimation();
        return styledDialog;
    }

    boolean isShowing() {
        return overlayRoot.getParent() != null;
    }

    void dismiss() {
        dismiss(null);
    }

    void dismiss(OnDismissListener onDismissListener) {
        if (!isShowing()) {
            return;
        }
        playExitAnimation(() -> {
            removeFromHost();
            removeBackCallback();
            if (onDismissListener != null) {
                onDismissListener.onDismiss();
            }
        });
    }

    private void removeFromHost() {
        ViewGroup parent = (ViewGroup) overlayRoot.getParent();
        if (parent != null) {
            parent.removeView(overlayRoot);
        }
    }

    private void removeBackCallback() {
        if (backCallback != null) {
            backCallback.remove();
            backCallback = null;
        }
    }

    private void playEnterAnimation() {
        cancelRunningAnimator();
        panel.animate().cancel();
        scrim.setAlpha(1f);

        if (style == DialogAnimationStyle.DROP_FROM_TOP) {
            playPathLeafEnter();
        } else {
            playPopupEnter();
        }
    }

    private void playPopupEnter() {
        panel.post(() -> {
            applyPivotCenter(panel);
            panel.setTranslationX(0f);
            panel.setTranslationY(0f);
            panel.setRotation(0f);
            panel.setScaleX(POPUP_START_SCALE);
            panel.setScaleY(POPUP_START_SCALE);
            panel.setAlpha(0f);

            panel.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(1f)
                    .setDuration(POPUP_ENTER_MS)
                    .setInterpolator(new OvershootInterpolator(POPUP_OVERSHOOT))
                    .start();
        });
    }

    /**
     * Path 风格：整张卡片一开始就排好版，像叶子从顶端飘落（带旋转），落地轻弹。
     * 必须在首帧绘制前移到屏外，否则会先闪一下再动画。
     */
    private void playPathLeafEnter() {
        resetContentTransforms(content);

        panel.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                panel.getViewTreeObserver().removeOnPreDrawListener(this);
                startPathLeafEnterAnimator();
                return true;
            }
        });
    }

    private void startPathLeafEnterAnimator() {
        int panelH = measuredPanelHeight(panel);
        final float startY = -(panelH + dp(panel.getContext(), 72));
        final float swayX = dp(panel.getContext(), LEAF_SWAY_X_DP);

        applyPivotBottomCenter(panel);
        panel.setTranslationX(0f);
        panel.setTranslationY(startY);
        panel.setRotation(LEAF_START_ROTATION);
        panel.setAlpha(1f);
        panel.setScaleX(1f);
        panel.setScaleY(1f);

        runningAnimator = ValueAnimator.ofFloat(0f, 1f);
        runningAnimator.setDuration(LEAF_ENTER_MS);
        runningAnimator.setInterpolator(new PathLeafFallInterpolator());
        runningAnimator.addUpdateListener(animation -> {
            float t = (float) animation.getAnimatedValue();
            panel.setTranslationY(startY * (1f - t));
            panel.setTranslationX(swayX * (float) Math.sin(Math.PI * t * 1.35f));
            panel.setRotation(
                    LEAF_START_ROTATION * (1f - t)
                            + LEAF_SWAY_ROTATION * (float) Math.sin(Math.PI * t) * (1f - t * 0.35f)
            );
        });
        runningAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                panel.setTranslationX(0f);
                panel.setTranslationY(0f);
                panel.setRotation(0f);
                runningAnimator = null;
            }
        });
        runningAnimator.start();
    }

    /** 加入视图树之前就移到屏外，避免 addView 后首帧落在目标位置。 */
    private static void applyPathLeafOffscreenStart(View panel, Context context) {
        int offscreenY = context.getResources().getDisplayMetrics().heightPixels;
        panel.setTranslationY(-offscreenY);
        panel.setTranslationX(0f);
        panel.setRotation(LEAF_START_ROTATION);
        panel.setAlpha(1f);
        panel.setScaleX(1f);
        panel.setScaleY(1f);
    }

    private void playExitAnimation(Runnable end) {
        cancelRunningAnimator();
        panel.animate().cancel();

        if (style == DialogAnimationStyle.DROP_FROM_TOP) {
            playPathLeafExit(end);
        } else {
            playPopupExit(end);
        }
    }

    private void playPopupExit(Runnable end) {
        panel.post(() -> {
            applyPivotCenter(panel);
            panel.animate()
                    .scaleX(POPUP_START_SCALE)
                    .scaleY(POPUP_START_SCALE)
                    .alpha(0f)
                    .setDuration(POPUP_EXIT_MS)
                    .setInterpolator(new DecelerateInterpolator(1.8f))
                    .withEndAction(end)
                    .start();
        });
    }

    /** 逆向飘回顶端消失。 */
    private void playPathLeafExit(Runnable end) {
        panel.post(() -> {
            final float startY = panel.getTranslationY();
            final float startX = panel.getTranslationX();
            final float startRot = panel.getRotation();
            int panelH = measuredPanelHeight(panel);
            final float endY = -(panelH + dp(panel.getContext(), 56));
            final float swayX = dp(panel.getContext(), LEAF_SWAY_X_DP);

            applyPivotBottomCenter(panel);

            runningAnimator = ValueAnimator.ofFloat(0f, 1f);
            runningAnimator.setDuration(LEAF_EXIT_MS);
            runningAnimator.setInterpolator(new DecelerateInterpolator(1.3f));
            runningAnimator.addUpdateListener(animation -> {
                float t = (float) animation.getAnimatedValue();
                panel.setTranslationY(startY + (endY - startY) * t);
                panel.setTranslationX(startX + (-swayX - startX) * t);
                panel.setRotation(startRot + (LEAF_START_ROTATION - startRot) * t);
                panel.setAlpha(1f - t * 0.35f);
            });
            runningAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    runningAnimator = null;
                    if (end != null) {
                        end.run();
                    }
                }
            });
            runningAnimator.start();
        });
    }

    private void cancelRunningAnimator() {
        if (runningAnimator != null) {
            runningAnimator.cancel();
            runningAnimator = null;
        }
    }

    private static void resetContentTransforms(View content) {
        if (!(content instanceof ViewGroup)) {
            return;
        }
        ViewGroup group = (ViewGroup) content;
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            child.setTranslationX(0f);
            child.setTranslationY(0f);
            child.setAlpha(1f);
            child.setScaleX(1f);
            child.setScaleY(1f);
            child.setRotation(0f);
        }
        content.setAlpha(1f);
        content.setTranslationX(0f);
        content.setTranslationY(0f);
        content.setRotation(0f);
        content.setScaleX(1f);
        content.setScaleY(1f);
    }

    private static void applyPivotCenter(View panel) {
        panel.setPivotX(panel.getWidth() / 2f);
        panel.setPivotY(panel.getHeight() / 2f);
    }

    private static void applyPivotBottomCenter(View panel) {
        panel.setPivotX(panel.getWidth() / 2f);
        panel.setPivotY(panel.getHeight());
    }

    private static int dp(android.content.Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private static int measuredPanelHeight(View panel) {
        if (panel.getHeight() > 0) {
            return panel.getHeight();
        }
        panel.measure(
                View.MeasureSpec.makeMeasureSpec(
                        panel.getResources().getDisplayMetrics().widthPixels,
                        View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        return panel.getMeasuredHeight();
    }

    private static int getStatusBarHeight(android.content.Context context) {
        int resId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resId > 0) {
            return context.getResources().getDimensionPixelSize(resId);
        }
        return dp(context, 24);
    }
}
