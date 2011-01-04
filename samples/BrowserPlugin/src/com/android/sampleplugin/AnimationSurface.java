package com.android.sampleplugin;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.widget.TextView;

public class AnimationSurface extends TextView {

    public AnimationSurface(Context context) {
        super(context);

        Log.e("AnimSurface", "IN ANIMATION SURFACE");
        
        this.setBackgroundColor(Color.GRAY);
        this.setTextColor(Color.WHITE);
        this.setText("This is a full-screen plugin");

        // ensure that the view system is aware that we will be drawing
        this.setWillNotDraw(false);
    }
}
