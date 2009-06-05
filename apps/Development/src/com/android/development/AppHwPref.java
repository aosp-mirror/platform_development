/*
**
** Copyright 2006, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

package com.android.development;

import com.android.development.R;
import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ConfigurationInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

/* This activity displays the hardware configuration details
 * of an application as defined in its manifests
 */
public class AppHwPref extends Activity {
    private static final String TAG = "AppHwPref";
    PackageManager mPm;
    private static final int BASE = 0;
    private static final int TOUCHSCREEN = BASE + 1;
    private static final int KEYBOARD_TYPE = BASE + 2;
    private static final int NAVIGATION = BASE + 3;
    private static final int GLES_VERSION = BASE + 4;
    
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Intent intent = getIntent();
        String pkgName = intent.getStringExtra("packageName");
        if(pkgName == null) {
           handleError("Null package name", true);
           return;
        }
        mPm = getPackageManager();
        PackageInfo pInfo;
        try {
            pInfo = mPm.getPackageInfo(pkgName, PackageManager.GET_CONFIGURATIONS);
        } catch (NameNotFoundException e) {
            pInfo = null;
        }
        if(pInfo == null) {
            handleError("Failed retrieving packageInfo for pkg:"+pkgName, true);
            return;
        }
        ConfigurationInfo appHwPref[] = pInfo.configPreferences;
        
        setContentView(R.layout.application_hw_pref);
        if(appHwPref != null) {
            displayTextView(R.id.attr_package, pInfo.applicationInfo.loadLabel(mPm));
            displayTextView(R.id.attr_touchscreen, appHwPref, TOUCHSCREEN);
            displayTextView(R.id.attr_input_method, appHwPref, KEYBOARD_TYPE);
            displayTextView(R.id.attr_navigation, appHwPref, NAVIGATION);
            displayFlag(R.id.attr_hard_keyboard, ConfigurationInfo.INPUT_FEATURE_HARD_KEYBOARD, appHwPref);
            displayFlag(R.id.attr_five_way_nav, ConfigurationInfo.INPUT_FEATURE_FIVE_WAY_NAV, appHwPref);
           displayTextView(R.id.attr_gles_version, appHwPref, GLES_VERSION);
        }
    }
    
    void displayFlag(int viewId, int flagMask, ConfigurationInfo[] appHwPref) {
        if(appHwPref == null) {
            return;
        }
        boolean flag = false;
        for (int i = 0; i < appHwPref.length; i++) {
            ConfigurationInfo pref = appHwPref[i];
            if((pref.reqInputFeatures & flagMask) != 0) {
                flag = true;
                break;
            }
        }
        if(flag) {
            displayTextView(viewId, "true");
        } else {
            displayTextView(viewId, "false");
        }
    }
    
    void handleError(String errMsg, boolean finish) {
        // TODO display dialog
        Log.i(TAG, errMsg);
        if(finish) {
            finish();
        }
    }
    
    void displayTextView(int textViewId, CharSequence displayStr) {
        TextView tView = (TextView) findViewById(textViewId);
        if(displayStr != null) {
            tView.setText(displayStr);
        }
    }
    
    void displayTextView(int viewId, ConfigurationInfo[] config, int type) {
        if((config == null) || (config.length < 1)) {
            return;
        }
        
        HashSet<String> list = new HashSet<String>();
        for(int i = 0; i < config.length; i++) {
            String str = null;
            switch(type) {
            case TOUCHSCREEN:
                str = getTouchScreenStr(config[i]);
                break;
            case KEYBOARD_TYPE:
                str =  getKeyboardTypeStr(config[i]);
                break;
            case NAVIGATION:
                str = getNavigationStr(config[i]);
                break;
            case GLES_VERSION:
                str = config[i].getGlEsVersion();
                break;
            }
            if(str != null) {
                list.add(str);
            }
        }
        String listStr = "";
        boolean set = false;
        for(String str : list) {
            set = true;
            listStr += str+",";
        }
        if(set) {
            TextView tView = (TextView)findViewById(viewId);
            CharSequence txt = listStr.subSequence(0, listStr.length()-1);
            tView.setText(txt);
        }
    }
    
    String getTouchScreenStr(ConfigurationInfo appHwPref) {
        if(appHwPref == null) {
            handleError("Invalid HardwareConfigurationObject", true);
            return null;
        }
        switch(appHwPref.reqTouchScreen) {
        case Configuration.TOUCHSCREEN_FINGER:
            return "finger";
        case Configuration.TOUCHSCREEN_NOTOUCH:
            return "notouch";
        case Configuration.TOUCHSCREEN_STYLUS:
            return "stylus";
        case Configuration.TOUCHSCREEN_UNDEFINED:
            return null;
        default:
                return null;
        }
    }
    
    String getKeyboardTypeStr(ConfigurationInfo appHwPref) {
        if(appHwPref == null) {
            handleError("Invalid HardwareConfigurationObject", true);
            return null;
        }
        switch(appHwPref.reqKeyboardType) {
        case Configuration.KEYBOARD_12KEY:
            return "12key";
        case Configuration.KEYBOARD_NOKEYS:
            return "nokeys";
        case Configuration.KEYBOARD_QWERTY:
            return "querty";
        case Configuration.KEYBOARD_UNDEFINED:
            return null;
        default:
                return null;
        }
    }
    
    String getNavigationStr(ConfigurationInfo appHwPref) {
        if(appHwPref == null) {
            handleError("Invalid HardwareConfigurationObject", true);
            return null;
        }
        switch(appHwPref.reqNavigation) {
        case Configuration.NAVIGATION_DPAD:
            return "dpad";
        case Configuration.NAVIGATION_TRACKBALL:
            return "trackball";
        case Configuration.NAVIGATION_WHEEL:
            return "wheel";
        case Configuration.NAVIGATION_UNDEFINED:
            return null;
        default:
                return null;
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}

