/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.development;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.TextView;

public class ConfigurationViewer extends Activity {
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.configuration_viewer);

        Configuration c = getResources().getConfiguration();
        DisplayMetrics m = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(m);

        TextView tv = (TextView)findViewById(R.id.text);

        String s = "Configuration\n"
                + "\n"
                + "fontScale=" + c.fontScale + "\n"
                + "hardKeyboardHidden=" + c.hardKeyboardHidden + "\n"
                + "keyboard=" + c.keyboard + "\n"
                + "locale=" + c.locale + "\n"
                + "mcc=" + c.mcc + "\n"
                + "mnc=" + c.mnc + "\n"
                + "navigation=" + c.navigation + "\n"
                + "navigationHidden=" + c.navigationHidden + "\n"
                + "orientation=" + c.orientation + "\n"
                + "screenLayout=0x" + Integer.toHexString(c.screenLayout) + "\n"
                + "touchscreen=" + c.touchscreen + "\n"
                + "uiMode=0x" + Integer.toHexString(c.uiMode) + "\n"
                + "\n"
                + "DisplayMetrics\n"
                + "\n"
                + "density=" + m.density + "\n"
                + "densityDpi=" + m.densityDpi + "\n"
                + "heightPixels=" + m.heightPixels + "\n"
                + "scaledDensity=" + m.scaledDensity + "\n"
                + "widthPixels=" + m.widthPixels + "\n"
                + "xdpi=" + m.xdpi + "\n"
                + "ydpi=" + m.ydpi + "\n"
                ;

        tv.setText(s);

        // Also log it for bugreport purposes.
        Log.d("ConfigurationViewer", s);
    }
}

