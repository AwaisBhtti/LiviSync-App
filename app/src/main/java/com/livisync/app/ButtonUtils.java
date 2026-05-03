package com.livisync.app;

import android.content.res.ColorStateList;
import android.view.View;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;

public class ButtonUtils {

    public static void setWorkingState(View view, boolean isWorking) {
        if (!(view instanceof MaterialButton)) return;
        
        MaterialButton button = (MaterialButton) view;
        if (isWorking) {
            button.setEnabled(false);
            button.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(view.getContext(), R.color.grey)));
        } else {
            button.setEnabled(true);
            button.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(view.getContext(), R.color.black)));
        }
    }
}
