/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.example.android.apis.app;

import com.example.android.apis.R;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioGroup;


public class RotationAnimation extends Activity {

    private int mRotationAnimation = LayoutParams.ROTATION_ANIMATION_ROTATE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRotationAnimation(mRotationAnimation);

        setContentView(R.layout.rotation_animation);

        ((CheckBox)findViewById(R.id.windowFullscreen)).setOnCheckedChangeListener(
            new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    setFullscreen(isChecked);
                }
            }
        );

        ((RadioGroup)findViewById(R.id.rotation_radio_group)).setOnCheckedChangeListener(
            new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    switch (checkedId) {
                        default:
                        case R.id.rotate:
                            mRotationAnimation = LayoutParams.ROTATION_ANIMATION_ROTATE;
                            break;
                        case R.id.crossfade:
                            mRotationAnimation = LayoutParams.ROTATION_ANIMATION_CROSSFADE;
                            break;
                        case R.id.jumpcut:
                            mRotationAnimation = LayoutParams.ROTATION_ANIMATION_JUMPCUT;
                            break;
                    }
                    setRotationAnimation(mRotationAnimation);
                }
            }
        );
    }

    private void setFullscreen(boolean on) {
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        if (on) {
            winParams.flags |=  WindowManager.LayoutParams.FLAG_FULLSCREEN;
        } else {
            winParams.flags &= ~WindowManager.LayoutParams.FLAG_FULLSCREEN;
        }
        win.setAttributes(winParams);
    }

    private void setRotationAnimation(int rotationAnimation) {
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        winParams.rotationAnimation = rotationAnimation;
        win.setAttributes(winParams);
    }
}
