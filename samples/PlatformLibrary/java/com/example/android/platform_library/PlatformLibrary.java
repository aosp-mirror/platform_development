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

package com.example.android.platform_library;

import android.util.Config;
import android.util.Log;

public final class PlatformLibrary {    
    static {
        /*
         * Load the library.  If it's already loaded, this does nothing.
         */
        System.loadLibrary("platform_library_jni");
    }

    private int mJniInt = -1;

    public PlatformLibrary() {}

    /*
     * Test native methods.
     */
    public int getInt(boolean bad) {
        /* this alters mJniInt */
        int result = getJniInt(bad);

        /* reverse a string, for no very good reason */
        String reverse = reverseString("Android!");

        Log.i("PlatformLibrary", "getInt: " + result + ", '" + reverse + "'");

        return mJniInt;
    }

    /*
     * Simple method, called from native code.
     */
    private static void yodel(String msg) {
        Log.d("PlatformLibrary", "yodel: " + msg);
    }

    /*
     * Trivial native method call.  If "bad" is true, this will throw an
     * exception.
     */
    native private int getJniInt(boolean bad);

    /*
     * Native method that returns a new string that is the reverse of
     * the original.  This also calls yodel().
     */
    native private static String reverseString(String str);
}
