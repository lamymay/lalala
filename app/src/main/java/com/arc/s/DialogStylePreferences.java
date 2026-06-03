package com.arc.s;

import android.content.Context;
import android.content.SharedPreferences;

final class DialogStylePreferences {

    private static final String PREF_NAME = "lalala_prefs";
    private static final String KEY_MENU_DIALOG_STYLE = "menu_dialog_style";

    private DialogStylePreferences() {
    }

    static DialogAnimationStyle getMenuDialogStyle(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        int ordinal = prefs.getInt(KEY_MENU_DIALOG_STYLE, DialogAnimationStyle.POPUP.ordinal());
        DialogAnimationStyle[] values = DialogAnimationStyle.values();
        if (ordinal < 0 || ordinal >= values.length) {
            return DialogAnimationStyle.POPUP;
        }
        return values[ordinal];
    }

    static void setMenuDialogStyle(Context context, DialogAnimationStyle style) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_MENU_DIALOG_STYLE, style.ordinal())
                .apply();
    }
}
