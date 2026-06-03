package com.arc.s;

/**
 * 长按菜单等弹窗的入场/退场动画风格。
 */
public enum DialogAnimationStyle {

    POPUP("中心点爆发"),
    DROP_FROM_TOP("Path 叶片飘落");

    private final String label;

    DialogAnimationStyle(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public DialogAnimationStyle next() {
        DialogAnimationStyle[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
