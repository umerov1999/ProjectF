package com.yalantis.ucrop.util;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;

/**
 * Hack class to properly support state drawable back to Android 1.6
 */
public class SelectedStateListDrawable extends StateListDrawable {

    private final int mSelectionColor;

    public SelectedStateListDrawable(Drawable drawable, int selectionColor) {
        mSelectionColor = selectionColor;
        addState(new int[]{android.R.attr.state_selected}, drawable);
        addState(new int[]{}, drawable);
    }

    @Override
    protected boolean onStateChange(int[] states) {
        boolean isStatePressedInArray = false;
        for (int state : states) {
            if (state == android.R.attr.state_selected) {
                isStatePressedInArray = true;
                break;
            }
        }
        if (isStatePressedInArray) {
            setColorFilter(mSelectionColor, PorterDuff.Mode.SRC_ATOP);
        } else {
            clearColorFilter();
        }
        return super.onStateChange(states);
    }

    @Override
    public boolean isStateful() {
        return true;
    }

}
