/* //device/apps/Settings/src/com/android/settings/Keyguard.java
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

import android.app.Activity;
import android.app.ActivityManagerNative;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.ServiceManagerNative;
import android.os.StrictMode;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.IWindowManager;
import android.view.View;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Map;

public class DevelopmentSettings extends Activity {
    private static final String TAG = "DevelopmentSettings";
    private static final int DEBUG_APP_REQUEST = 1;

    private Button mDebugAppButton;
    private CheckBox mWaitForDebuggerCB;
    private CheckBox mAlwaysFinishCB;
    private Spinner mPointerLocationSpinner;
    private Spinner mStrictModeVisualSpinner;
    private CheckBox mShowLoadCB;
    private CheckBox mShowCpuCB;
    private CheckBox mEnableGLCB;
    private CheckBox mShowUpdatesCB;
    private CheckBox mShowBackgroundCB;
    private CheckBox mShowSleepCB;
    private CheckBox mShowXmppCB;
    private CheckBox mWindowOrientationListenerLogCB;
    private CheckBox mCompatibilityModeCB;
    private Spinner mMaxProcsSpinner;
    private Spinner mWindowAnimationScaleSpinner;
    private Spinner mTransitionAnimationScaleSpinner;
    private Spinner mFontHintingSpinner;

    private String mDebugApp;
    private boolean mWaitForDebugger;
    private boolean mAlwaysFinish;
    private int mPointerLocation;
    private int mWindowOrientationListenerLog;
    private int mProcessLimit;
    private boolean mShowSleep;
    private boolean mShowXmpp;
    private boolean mCompatibilityMode;
    private AnimationScaleSelectedListener mWindowAnimationScale
            = new AnimationScaleSelectedListener(0);
    private AnimationScaleSelectedListener mTransitionAnimationScale
            = new AnimationScaleSelectedListener(1);
    private SharedPreferences mSharedPrefs;
    private IWindowManager mWindowManager;

    private static final boolean FONT_HINTING_ENABLED = true;
    private static final String  FONT_HINTING_FILE = "/data/misc/font-hack";

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.development_settings);

        mDebugAppButton = (Button)findViewById(R.id.debug_app);
        mDebugAppButton.setOnClickListener(mDebugAppClicked);
        mWaitForDebuggerCB = (CheckBox)findViewById(R.id.wait_for_debugger);
        mWaitForDebuggerCB.setOnClickListener(mWaitForDebuggerClicked);
        mAlwaysFinishCB = (CheckBox)findViewById(R.id.always_finish);
        mAlwaysFinishCB.setOnClickListener(mAlwaysFinishClicked);
        mPointerLocationSpinner = (Spinner)findViewById(R.id.pointer_location);
        mPointerLocationSpinner.setOnItemSelectedListener(mPointerLocationChanged);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                new String[] {
                        "No Pointer Location",
                        "Pointer Location" });
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mPointerLocationSpinner.setAdapter(adapter);
        mStrictModeVisualSpinner = (Spinner)findViewById(R.id.strictmode_visual);
        adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                new String[] {
                        "StrictMode visual indicator: build variant default",
                        "StrictMode visual indicator: on",
                        "StrictMode visual indicator: off" });
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mStrictModeVisualSpinner.setAdapter(adapter);
        mStrictModeVisualSpinner.setOnItemSelectedListener(mStrictModeVisualChanged);
        mShowLoadCB = (CheckBox)findViewById(R.id.show_load);
        mShowLoadCB.setOnClickListener(mShowLoadClicked);
        mShowCpuCB = (CheckBox)findViewById(R.id.show_cpu);
        mShowCpuCB.setOnCheckedChangeListener(new SurfaceFlingerClicker(1000));
        mEnableGLCB = (CheckBox)findViewById(R.id.enable_gl);
        mEnableGLCB.getLayoutParams().height = 0; // doesn't do anything
        mEnableGLCB.setOnCheckedChangeListener(new SurfaceFlingerClicker(1004));
        mShowUpdatesCB = (CheckBox)findViewById(R.id.show_updates);
        mShowUpdatesCB.setOnCheckedChangeListener(new SurfaceFlingerClicker(1002));
        mShowBackgroundCB = (CheckBox)findViewById(R.id.show_background);
        mShowBackgroundCB.setOnCheckedChangeListener(new SurfaceFlingerClicker(1003));
        mShowSleepCB = (CheckBox)findViewById(R.id.show_sleep);
        mShowSleepCB.setOnClickListener(mShowSleepClicked);
        mShowXmppCB = (CheckBox)findViewById(R.id.show_xmpp);
        mShowXmppCB.setOnClickListener(mShowXmppClicked);
        mWindowOrientationListenerLogCB = (CheckBox)findViewById(R.id.window_orientation_listener_log);
        mWindowOrientationListenerLogCB.setOnClickListener(mWindowOrientationListenerLogClicked);
        mCompatibilityModeCB = (CheckBox)findViewById(R.id.compatibility_mode);
        mCompatibilityModeCB.setOnClickListener(mCompatibilityModeClicked);
        mMaxProcsSpinner = (Spinner)findViewById(R.id.max_procs);
        mMaxProcsSpinner.setOnItemSelectedListener(mMaxProcsChanged);
        adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                new String[] {
                        "No App Process Limit",
                        "Max 1 App Process",
                        "Max 2 App Processes",
                        "Max 3 App Processes",
                        "Max 4 App Processes" });
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mMaxProcsSpinner.setAdapter(adapter);
        mWindowAnimationScaleSpinner = setupAnimationSpinner(
                R.id.window_animation_scale, mWindowAnimationScale, "Window");
        mTransitionAnimationScaleSpinner = setupAnimationSpinner(
                R.id.transition_animation_scale, mTransitionAnimationScale, "Transition");

        if (FONT_HINTING_ENABLED) {
            mFontHintingSpinner = (Spinner)findViewById(R.id.font_hinting);
            mFontHintingSpinner.setOnItemSelectedListener(mFontHintingChanged);
            adapter = new ArrayAdapter<String>(
                    this,
                    android.R.layout.simple_spinner_item,
                    new String[] {
                            "Light Hinting",
                            "Medium Hinting" });
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mFontHintingSpinner.setAdapter(adapter);
        }
        mSharedPrefs = getSharedPreferences("global", 0);
        mWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
    }

    Spinner setupAnimationSpinner(int resid,
            AnimationScaleSelectedListener listener, String name) {
        Spinner spinner = (Spinner)findViewById(resid);
        spinner.setOnItemSelectedListener(listener);
        ArrayAdapter adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                new String[] {
                        name + " Animation Scale 1x",
                        name + " Animation Scale 2x",
                        name + " Animation Scale 5x",
                        name + " Animation Scale 10x",
                        name + " Animation Off" });
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        listener.spinner = spinner;
        return spinner;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        updateDebugOptions();
        updateFinishOptions();
        updatePointerLocationOptions();
        updateStrictModeVisualOptions();
        updateProcessLimitOptions();
        updateSharedOptions();
        updateFlingerOptions();
        updateSleepOptions();
        updateXmppOptions();
        updateWindowOrientationListenerLogOptions();
        updateCompatibilityOptions();

        try {
            FileInputStream  in = new FileInputStream( FONT_HINTING_FILE );
            int    mode = in.read() - 48;
            if (mode >= 0 && mode < 3)
                mFontHintingSpinner.setSelection(mode);
            in.close();
        } catch (Exception e) {
        }

        mWindowAnimationScale.load();
        mTransitionAnimationScale.load();
    }

    private void writeDebugOptions() {
        try {
            ActivityManagerNative.getDefault().setDebugApp(
                mDebugApp, mWaitForDebugger, true);
        } catch (RemoteException ex) {
        }
    }

    private void updateDebugOptions() {
        mDebugApp = Settings.System.getString(
            getContentResolver(), Settings.System.DEBUG_APP);
        mWaitForDebugger = Settings.System.getInt(
            getContentResolver(), Settings.System.WAIT_FOR_DEBUGGER, 0) != 0;

        mDebugAppButton.setText(
            mDebugApp == null || mDebugApp.length() == 0 ? "(none)" : mDebugApp);
        mWaitForDebuggerCB.setChecked(mWaitForDebugger);
    }

    private void writeFinishOptions() {
        try {
            ActivityManagerNative.getDefault().setAlwaysFinish(mAlwaysFinish);
        } catch (RemoteException ex) {
        }
    }

    private void updateFinishOptions() {
        mAlwaysFinish = Settings.System.getInt(
            getContentResolver(), Settings.System.ALWAYS_FINISH_ACTIVITIES, 0) != 0;
        mAlwaysFinishCB.setChecked(mAlwaysFinish);
    }

    private void writePointerLocationOptions() {
        Settings.System.putInt(getContentResolver(),
                Settings.System.POINTER_LOCATION, mPointerLocation);
    }

    private void updatePointerLocationOptions() {
        mPointerLocation = Settings.System.getInt(getContentResolver(),
                Settings.System.POINTER_LOCATION, 0);
        mPointerLocationSpinner.setSelection(mPointerLocation);
    }

    private void writeWindowOrientationListenerLogOptions() {
        Settings.System.putInt(getContentResolver(),
                Settings.System.WINDOW_ORIENTATION_LISTENER_LOG, mWindowOrientationListenerLog);
    }

    private void updateWindowOrientationListenerLogOptions() {
        mWindowOrientationListenerLog = Settings.System.getInt(getContentResolver(),
                Settings.System.WINDOW_ORIENTATION_LISTENER_LOG, 0);
        mWindowOrientationListenerLogCB.setChecked(mWindowOrientationListenerLog != 0);
    }

    // Returns the current state of the system property that controls
    // strictmode flashes.  One of:
    //    0: not explicitly set one way or another
    //    1: on
    //    2: off
    // These are the indices in the Spinner's ArrayAdapter.
    private int currentStrictModeActiveIndex() {
        if (TextUtils.isEmpty(SystemProperties.get(StrictMode.VISUAL_PROPERTY))) {
            return 0;
        }
        boolean enabled = SystemProperties.getBoolean(StrictMode.VISUAL_PROPERTY, false);
        return enabled ? 1 : 2;
    }

    private void updateStrictModeVisualOptions() {
        mStrictModeVisualSpinner.setSelection(currentStrictModeActiveIndex());
    }

    private void writeProcessLimitOptions() {
        try {
            ActivityManagerNative.getDefault().setProcessLimit(mProcessLimit);
        } catch (RemoteException ex) {
        }
    }

    private void updateProcessLimitOptions() {
        try {
            mProcessLimit = ActivityManagerNative.getDefault().getProcessLimit();
            mMaxProcsSpinner.setSelection(mProcessLimit);
        } catch (RemoteException ex) {
        }
    }

    private void updateSharedOptions() {
        mShowLoadCB.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.SHOW_PROCESSES, 0) != 0);
    }

    private void writeCompatibilityOptions() {
        Settings.System.putInt(getContentResolver(),
                Settings.System.COMPATIBILITY_MODE, mCompatibilityMode ? 0 : 1);
    }

    private void updateCompatibilityOptions() {
        mCompatibilityMode = Settings.System.getInt(
            getContentResolver(), Settings.System.COMPATIBILITY_MODE, 1) == 0;
        mCompatibilityModeCB.setChecked(mCompatibilityMode);
    }

    private void updateFlingerOptions() {
        // magic communication with surface flinger.
        try {
            IBinder flinger = ServiceManager.getService("SurfaceFlinger");
            if (flinger != null) {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                data.writeInterfaceToken("android.ui.ISurfaceComposer");
                flinger.transact(1010, data, reply, 0);
                int v;
                v = reply.readInt();
                mShowCpuCB.setChecked(v != 0);
                v = reply.readInt();
                mEnableGLCB.setChecked(v != 0);
                v = reply.readInt();
                mShowUpdatesCB.setChecked(v != 0);
                v = reply.readInt();
                mShowBackgroundCB.setChecked(v != 0);
                reply.recycle();
                data.recycle();
            }
        } catch (RemoteException ex) {
        }
    }

    private void writeSleepOptions() {
        try {
            FileOutputStream os = new FileOutputStream(
                "/sys/devices/platform/gpio_sleep_debug/enable", true);
            if(mShowSleep)
                os.write(new byte[] { (byte)'1' });
            else
                os.write(new byte[] { (byte)'0' });
            os.close();
        } catch (Exception e) {
            Log.w(TAG, "Failed setting gpio_sleep_debug");
        }
    }

    private void updateSleepOptions() {
        try {
            FileInputStream is = new FileInputStream(
                "/sys/devices/platform/gpio_sleep_debug/enable");
            int character = is.read();
            mShowSleep = character == '1';
            is.close();
        } catch (Exception e) {
            Log.w(TAG, "Failed reading gpio_sleep_debug");
            mShowSleep = false;
        }
        mShowSleepCB.setChecked(mShowSleep);
    }

    private void writeXmppOptions() {
        Settings.System.setShowGTalkServiceStatus(getContentResolver(), mShowXmpp);
    }

    private void updateXmppOptions() {
        mShowXmpp = Settings.System.getShowGTalkServiceStatus(getContentResolver());
        mShowXmppCB.setChecked(mShowXmpp);
    }

    private View.OnClickListener mDebugAppClicked = new View.OnClickListener() {
        public void onClick(View v) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setClass(DevelopmentSettings.this, AppPicker.class);
            startActivityForResult(intent, DEBUG_APP_REQUEST);
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == DEBUG_APP_REQUEST && resultCode == RESULT_OK) {
            mDebugApp = intent.getAction();
            writeDebugOptions();
            updateDebugOptions();
        }
    }

    private View.OnClickListener mWaitForDebuggerClicked =
            new View.OnClickListener() {
        public void onClick(View v) {
            mWaitForDebugger = ((CheckBox)v).isChecked();
            writeDebugOptions();
            updateDebugOptions();
        }
    };

    private View.OnClickListener mAlwaysFinishClicked =
            new View.OnClickListener() {
        public void onClick(View v) {
            mAlwaysFinish = ((CheckBox)v).isChecked();
            writeFinishOptions();
            updateFinishOptions();
        }
    };

    private View.OnClickListener mCompatibilityModeClicked =
        new View.OnClickListener() {
    public void onClick(View v) {
        mCompatibilityMode = ((CheckBox)v).isChecked();
        writeCompatibilityOptions();
        updateCompatibilityOptions();
        Toast toast = Toast.makeText(DevelopmentSettings.this,
                R.string.development_settings_compatibility_mode_toast,
                Toast.LENGTH_LONG);
        toast.show();
    }
};

    private View.OnClickListener mShowLoadClicked = new View.OnClickListener() {
        public void onClick(View v) {
            boolean value = ((CheckBox)v).isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.SHOW_PROCESSES, value ? 1 : 0);
            Intent service = (new Intent())
                    .setClassName("android", "com.android.server.LoadAverageService");
            if (value) {
                startService(service);
            } else {
                stopService(service);
            }
        }
    };

    private class SurfaceFlingerClicker implements CheckBox.OnCheckedChangeListener {
        SurfaceFlingerClicker(int code) {
            mCode = code;
        }

        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            try {
                IBinder flinger = ServiceManager.getService("SurfaceFlinger");
                if (flinger != null) {
                    Parcel data = Parcel.obtain();
                    data.writeInterfaceToken("android.ui.ISurfaceComposer");
                    data.writeInt(isChecked ? 1 : 0);
                    flinger.transact(mCode, data, null, 0);
                    data.recycle();

                    updateFlingerOptions();
                }
            } catch (RemoteException ex) {
            }
        }

        final int mCode;
    }

    private View.OnClickListener mShowSleepClicked =
            new View.OnClickListener() {
        public void onClick(View v) {
            mShowSleep = ((CheckBox)v).isChecked();
            writeSleepOptions();
            updateSleepOptions();
        }
    };

    private View.OnClickListener mShowXmppClicked = new View.OnClickListener() {
        public void onClick(View v) {
            mShowXmpp = ((CheckBox)v).isChecked();
            // can streamline these calls, but keeping consistent with the
            // other development settings code.
            writeXmppOptions();
            updateXmppOptions();
        }
    };

    private View.OnClickListener mWindowOrientationListenerLogClicked = new View.OnClickListener() {
        public void onClick(View v) {
            mWindowOrientationListenerLog = ((CheckBox)v).isChecked() ? 1 : 0;
            writeWindowOrientationListenerLogOptions();
        }
    };

    private Spinner.OnItemSelectedListener mPointerLocationChanged
                                    = new Spinner.OnItemSelectedListener() {
        public void onItemSelected(android.widget.AdapterView av, View v,
                                    int position, long id) {
            mPointerLocation = position;
            writePointerLocationOptions();
        }

        public void onNothingSelected(android.widget.AdapterView av) {
        }
    };

    private Spinner.OnItemSelectedListener mStrictModeVisualChanged
                                    = new Spinner.OnItemSelectedListener() {
        public void onItemSelected(android.widget.AdapterView av, View v,
                                    int position, long id) {
            if (position == currentStrictModeActiveIndex()) {
                // at the existing position, so don't show a Toast.
                return;
            }

            try {
                switch (position) {
                    case 0:  // default
                        mWindowManager.setStrictModeVisualIndicatorPreference("");
                        break;
                    case 1:  // on
                        mWindowManager.setStrictModeVisualIndicatorPreference("1");
                        break;
                    case 2:  // off
                        mWindowManager.setStrictModeVisualIndicatorPreference("0");
                        break;
                }
            } catch (RemoteException e) {
                Log.w(TAG, "Error calling setStrictModeVisualIndicatorPreference", e);
            }

            Toast.makeText(
                    DevelopmentSettings.this,
                    "Setting changed; will take effect per-app next launch, or on reboot",
                    Toast.LENGTH_LONG).show();
        }

        public void onNothingSelected(android.widget.AdapterView av) {
        }
    };

    private Spinner.OnItemSelectedListener mMaxProcsChanged
                                    = new Spinner.OnItemSelectedListener() {
        public void onItemSelected(android.widget.AdapterView av, View v,
                                    int position, long id) {
            mProcessLimit = position;
            writeProcessLimitOptions();
        }

        public void onNothingSelected(android.widget.AdapterView av) {
        }
    };

    private Spinner.OnItemSelectedListener mFontHintingChanged
                                    = new Spinner.OnItemSelectedListener() {
        public void onItemSelected(android.widget.AdapterView  av, View v,
                                    int position, long id) {
            try {
                FileOutputStream  out = new FileOutputStream( FONT_HINTING_FILE );
                out.write(position+48);
                out.close();
            } catch (Exception e) {
                Log.w(TAG, "Failed to write font hinting settings to /data/misc/font-hack");
            }
        }

        public void onNothingSelected(android.widget.AdapterView av) {
        }
    };

    class AnimationScaleSelectedListener implements OnItemSelectedListener {
        final int which;
        float scale;
        Spinner spinner;
        
        AnimationScaleSelectedListener(int _which) {
            which = _which;
        }
        
        void load() {
            try {
                scale = mWindowManager.getAnimationScale(which);

                if (scale > 0.1f && scale < 2.0f) {
                    spinner.setSelection(0);
                } else if (scale >= 2.0f && scale < 3.0f) {
                    spinner.setSelection(1);
                } else if (scale >= 4.9f && scale < 6.0f) {
                    spinner.setSelection(2);
                }  else if (scale >= 9.9f && scale < 11.0f) {
                    spinner.setSelection(3);
                } else {
                    spinner.setSelection(4);
                }
            } catch (RemoteException e) {
            }
        }
        
        public void onItemSelected(android.widget.AdapterView av, View v,
                int position, long id) {
            switch (position) {
                case 0: scale = 1.0f; break;
                case 1: scale = 2.0f; break;
                case 2: scale = 5.0f; break;
                case 3: scale = 10.0f; break;
                case 4: scale = 0.0f; break;
                default: break;
            }

            try {
                mWindowManager.setAnimationScale(which, scale);
            } catch (RemoteException e) {
            }
        }

        public void onNothingSelected(android.widget.AdapterView av) {
        }
    }
}
