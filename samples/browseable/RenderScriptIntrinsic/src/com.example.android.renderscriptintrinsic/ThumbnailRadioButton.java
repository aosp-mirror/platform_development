/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.renderscriptintrinsic;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.Build;
import android.view.Gravity;
import android.widget.RadioButton;
import android.content.Context;
import android.util.AttributeSet;

/*
 A button with Thumbnail which extends Radio Button.
 The widget override a background drawable of Radio Button with a StateList Drawable.
 Each state has a LayerDrawable with a Thumbnail image and a Focus rectangle.
 It's using original Radio Buttons text as a label, because LayerDrawable showed some issues with Canvas.drawText().
 */
public class ThumbnailRadioButton extends RadioButton {
    public ThumbnailRadioButton(Context context) {
        super(context);
        init();
    }

    public ThumbnailRadioButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ThumbnailRadioButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setButtonDrawable(android.R.color.transparent);
    }

    public void setThumbnail(Bitmap bitmap) {
        //Bitmap drawable
        BitmapDrawable bmp = new BitmapDrawable(getResources(), bitmap);
        bmp.setGravity(Gravity.CENTER);

        int strokeWidth = 24;
        //Checked state
        ShapeDrawable rectChecked = new ShapeDrawable(new RectShape());
        rectChecked.getPaint().setColor(0xFFFFFFFF);
        rectChecked.getPaint().setStyle(Paint.Style.STROKE);
        rectChecked.getPaint().setStrokeWidth(strokeWidth);
        rectChecked.setIntrinsicWidth(bitmap.getWidth() + strokeWidth);
        rectChecked.setIntrinsicHeight(bitmap.getHeight() + strokeWidth);
        Drawable drawableArray[] = new Drawable[]{bmp, rectChecked};
        LayerDrawable layerChecked = new LayerDrawable(drawableArray);

        //Unchecked state
        ShapeDrawable rectUnchecked = new ShapeDrawable(new RectShape());
        rectUnchecked.getPaint().setColor(0x0);
        rectUnchecked.getPaint().setStyle(Paint.Style.STROKE);
        rectUnchecked.getPaint().setStrokeWidth(strokeWidth);
        rectUnchecked.setIntrinsicWidth(bitmap.getWidth() + strokeWidth);
        rectUnchecked.setIntrinsicHeight(bitmap.getHeight() + strokeWidth);
        Drawable drawableArray2[] = new Drawable[]{bmp, rectUnchecked};
        LayerDrawable layerUnchecked = new LayerDrawable(drawableArray2);

        //Statelist drawable
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_checked},
                layerChecked);
        states.addState(new int[]{},
                layerUnchecked);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            setBackground(states);
        else
            setBackgroundDrawable(states);

        //Offset text to center/bottom of the checkbox
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setTextSize(getTextSize());
        paint.setTypeface(getTypeface());
        float w = paint.measureText(getText(), 0, getText().length());
        setPadding(getPaddingLeft() + (int) ((bitmap.getWidth() - w) / 2.f + .5f),
                getPaddingTop() + (int) (bitmap.getHeight() * 0.70),
                getPaddingRight(),
                getPaddingBottom());

        setShadowLayer(5, 0, 0, Color.BLACK);
    }
}
