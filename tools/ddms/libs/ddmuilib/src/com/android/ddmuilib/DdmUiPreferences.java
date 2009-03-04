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

package com.android.ddmuilib;

import org.eclipse.jface.preference.IPreferenceStore;

/**
 * Preference entry point for ddmuilib. Allows the lib to access a preference
 * store (org.eclipse.jface.preference.IPreferenceStore) defined by the
 * application that includes the lib.
 */
public final class DdmUiPreferences {

    public static final int DEFAULT_THREAD_REFRESH_INTERVAL = 4;  // seconds

    private static int sThreadRefreshInterval = DEFAULT_THREAD_REFRESH_INTERVAL;
    
    private static IPreferenceStore mStore;
    
    private static String sSymbolLocation =""; //$NON-NLS-1$
    private static String sAddr2LineLocation =""; //$NON-NLS-1$
    private static String sTraceviewLocation =""; //$NON-NLS-1$

    public static void setStore(IPreferenceStore store) {
        mStore = store;
    }
    
    public static IPreferenceStore getStore() {
        return mStore;
    }

    public static int getThreadRefreshInterval() {
        return sThreadRefreshInterval;
    }

    public static void setThreadRefreshInterval(int port) {
        sThreadRefreshInterval = port;
    }
    
    static String getSymbolDirectory() {
        return sSymbolLocation;
    }

    public static void setSymbolsLocation(String location) {
        sSymbolLocation = location;
    }

    static String getAddr2Line() {
        return sAddr2LineLocation;
    }

    public static void setAddr2LineLocation(String location) {
        sAddr2LineLocation = location;
    }

    public static String getTraceview() {
        return sTraceviewLocation;
    }

    public static void setTraceviewLocation(String location) {
        sTraceviewLocation = location;
    }


}
