package com.android.sampleplugin;

import android.content.Context;
import android.graphics.Color;
import android.widget.TextView;

public class BackgroundSurface extends TextView {

    public BackgroundSurface(Context context) {
        super(context);

        this.setBackgroundColor(Color.BLACK);
        this.setTextColor(Color.WHITE);
        this.setText("This is a java background plugin");

        // ensure that the view system is aware that we will be drawing
        this.setWillNotDraw(false);
    }
}
