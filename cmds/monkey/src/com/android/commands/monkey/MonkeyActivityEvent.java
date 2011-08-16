/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.commands.monkey;

import android.app.IActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.IWindowManager;

/**
 * monkey activity event
 */
public class MonkeyActivityEvent extends MonkeyEvent {    
    private ComponentName mApp; 
    long mAlarmTime = 0;
    
    public MonkeyActivityEvent(ComponentName app) {
        super(EVENT_TYPE_ACTIVITY);
        mApp = app;
    }

    public MonkeyActivityEvent(ComponentName app, long arg) {
        super(EVENT_TYPE_ACTIVITY);
        mApp = app;
        mAlarmTime = arg;
    }

    /**
     * @return Intent for the new activity
     */
    private Intent getEvent() {        
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setComponent(mApp);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);        
        return intent;
    }

    @Override
    public int injectEvent(IWindowManager iwm, IActivityManager iam, int verbose) {
        Intent intent = getEvent();
        if (verbose > 0) {
            System.out.println(":Switch: " + intent.toUri(0));
        }

        if (mAlarmTime != 0){
            Bundle args = new Bundle();
            args.putLong("alarmTime", mAlarmTime);
            intent.putExtras(args);
        }

        try {
            iam.startActivity(null, intent, null, null, 0, null, null, 0,
                    false, false, null, null, false);
        } catch (RemoteException e) {
            System.err.println("** Failed talking with activity manager!");
            return MonkeyEvent.INJECT_ERROR_REMOTE_EXCEPTION;
        } catch (SecurityException e) {
            if (verbose > 0) {
                System.out.println("** Permissions error starting activity "
                        + intent.toUri(0));
            }
            return MonkeyEvent.INJECT_ERROR_SECURITY_EXCEPTION;
        }
        return MonkeyEvent.INJECT_SUCCESS;
    }
}
