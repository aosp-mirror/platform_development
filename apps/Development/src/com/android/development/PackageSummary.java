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
import android.app.ActivityManagerNative;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.InstrumentationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;


public class PackageSummary extends Activity {

    String mPackageName;
    private TextView mPackage;
    private ImageView mIconImage;
    private TextView mClass;
    private TextView mLabel;
    private View mDisabled;
    private View mSystem;
    private View mDebuggable;
    private View mNoCode;
    private View mPersistent;
    private Button mRestart;
    private TextView mTask;
    private TextView mVersion;
    private TextView mProcess;
    private TextView mUid;
    private TextView mSource;
    private TextView mData;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.package_summary);

        final PackageManager pm = getPackageManager();

        mPackage = (TextView)findViewById(R.id.packageView);
        mIconImage = (ImageView)findViewById(R.id.icon);
        mClass = (TextView)findViewById(R.id.classView);
        mLabel = (TextView)findViewById(R.id.label);
        mDisabled = findViewById(R.id.disabled);
        mSystem = findViewById(R.id.system);
        mDebuggable = findViewById(R.id.debuggable);
        mNoCode = findViewById(R.id.nocode);
        mPersistent = findViewById(R.id.persistent);
        mRestart = (Button)findViewById(R.id.restart);
        mTask = (TextView)findViewById(R.id.task);
        mVersion = (TextView)findViewById(R.id.version);
        mUid = (TextView)findViewById(R.id.uid);
        mProcess = (TextView)findViewById(R.id.process);
        mSource = (TextView)findViewById(R.id.source);
        mData = (TextView)findViewById(R.id.data);

        mPackageName = getIntent().getData().getSchemeSpecificPart();
        PackageInfo info = null;
        try {
            info = pm.getPackageInfo(mPackageName,
                PackageManager.GET_ACTIVITIES | PackageManager.GET_RECEIVERS
                | PackageManager.GET_SERVICES | PackageManager.GET_PROVIDERS
                | PackageManager.GET_INSTRUMENTATION
                | PackageManager.GET_DISABLED_COMPONENTS);
        } catch (PackageManager.NameNotFoundException e) {
        }

        if (info != null) {
            mPackage.setText(info.packageName);
            CharSequence label = null;
            String appClass = null;
            if (info.applicationInfo != null) {
                mIconImage.setImageDrawable(
                    pm.getApplicationIcon(info.applicationInfo));
                label = info.applicationInfo.nonLocalizedLabel;
                appClass = info.applicationInfo.className;
                if (info.applicationInfo.enabled) {
                    mDisabled.setVisibility(View.GONE);
                }
                if ((info.applicationInfo.flags&ApplicationInfo.FLAG_SYSTEM) == 0) {
                    mSystem.setVisibility(View.GONE);
                }
                if ((info.applicationInfo.flags&ApplicationInfo.FLAG_DEBUGGABLE) == 0) {
                    mDebuggable.setVisibility(View.GONE);
                }
                if ((info.applicationInfo.flags&ApplicationInfo.FLAG_HAS_CODE) != 0) {
                    mNoCode.setVisibility(View.GONE);
                }
                if ((info.applicationInfo.flags&ApplicationInfo.FLAG_PERSISTENT) == 0) {
                    mPersistent.setVisibility(View.GONE);
                }
                mUid.setText(Integer.toString(info.applicationInfo.uid));
                mProcess.setText(info.applicationInfo.processName);
                if (info.versionName != null) {
                    mVersion.setText(info.versionName + " (#" + info.versionCode + ")");
                } else {
                    mVersion.setText("(#" + info.versionCode + ")");
                }
                mSource.setText(info.applicationInfo.sourceDir);
                mData.setText(info.applicationInfo.dataDir);
                if (info.applicationInfo.taskAffinity != null) {
                    mTask.setText("\"" + info.applicationInfo.taskAffinity + "\"");
                } else {
                    mTask.setText("(No Task Affinity)");
                }
            }
            if (appClass != null) {
                if (appClass.startsWith(info.packageName + "."))
                    mClass.setText(appClass.substring(info.packageName.length()));
                else
                    mClass.setText(appClass);
            } else {
                mClass.setText("(No Application Class)");
            }
            if (label != null) {
                mLabel.setText("\"" + label + "\"");
            } else {
                mLabel.setText("(No Label)");
            }

            mRestart.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    try {
                        ActivityManagerNative.getDefault().killBackgroundProcesses(mPackageName);
                    } catch (RemoteException e) {
                    }
                }
            });
            
            final LayoutInflater inflate =
                (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
            LinearLayout activities = (LinearLayout)findViewById(R.id.activities);
            LinearLayout receivers = (LinearLayout)findViewById(R.id.receivers);
            LinearLayout services = (LinearLayout)findViewById(R.id.services);
            LinearLayout providers = (LinearLayout)findViewById(R.id.providers);
            LinearLayout instrumentation = (LinearLayout)findViewById(R.id.instrumentation);

            if (info.activities != null) {
                final int N = info.activities.length;
                for (int i=0; i<N; i++) {
                    ActivityInfo ai = info.activities[i];
                    // If an activity is disabled then the ActivityInfo will be null 
                    if (ai != null) {
                        Button view = (Button)inflate.inflate(
                                R.layout.package_item, null, false);
                        view.setOnClickListener(new ActivityOnClick(
                                new ComponentName(ai.applicationInfo.packageName,
                                                  ai.name)));
                        setItemText(view, info, ai.name);
                        activities.addView(view, lp);
                    }
                }
            } else {
                activities.setVisibility(View.GONE);
            }

            if (info.receivers != null) {
                final int N = info.receivers.length;
                for (int i=0; i<N; i++) {
                    ActivityInfo ai = info.receivers[i];
                    Button view = (Button)inflate.inflate(
                        R.layout.package_item, null, false);
                    Log.i("foo", "Receiver #" + i + " of " + N + ": " + ai);
                    setItemText(view, info, ai.name);
                    receivers.addView(view, lp);
                }
            } else {
                receivers.setVisibility(View.GONE);
            }

            if (info.services != null) {
                final int N = info.services.length;
                for (int i=0; i<N; i++) {
                    ServiceInfo si = info.services[i];
                    Button view = (Button)inflate.inflate(
                        R.layout.package_item, null, false);
                    setItemText(view, info, si.name);
                    services.addView(view, lp);
                }
            } else {
                services.setVisibility(View.GONE);
            }

            if (info.providers != null) {
                final int N = info.providers.length;
                for (int i=0; i<N; i++) {
                    ProviderInfo pi = info.providers[i];
                    Button view = (Button)inflate.inflate(
                        R.layout.package_item, null, false);
                    setItemText(view, info, pi.name);
                    providers.addView(view, lp);
                }
            } else {
                providers.setVisibility(View.GONE);
            }

            if (info.instrumentation != null) {
                final int N = info.instrumentation.length;
                for (int i=0; i<N; i++) {
                    InstrumentationInfo ii = info.instrumentation[i];
                    Button view = (Button)inflate.inflate(
                        R.layout.package_item, null, false);
                    setItemText(view, info, ii.name);
                    instrumentation.addView(view, lp);
                }
            } else {
                instrumentation.setVisibility(View.GONE);
            }

        }
        
        // Put focus here, so a button doesn't get focus and cause the
        // scroll view to move to it.
        mPackage.requestFocus();
    }

    private final static void setItemText(Button item, PackageInfo pi,
                                          String className)
    {
        item.setText(className.substring(className.lastIndexOf('.')+1));
    }

    private final class ActivityOnClick implements View.OnClickListener
    {
        private final ComponentName mClassName;
        ActivityOnClick(ComponentName className) {
            mClassName = className;
        }

        public void onClick(View v) {
            Intent intent = new Intent(
                null, Uri.fromParts("component",
                    mClassName.flattenToString(), null));
            intent.setClass(PackageSummary.this, ShowActivity.class);
            startActivity(intent);
        }
    }
}
