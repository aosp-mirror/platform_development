/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.emulator.connectivity.test;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.test.AndroidTestCase;
import android.util.Log;

/**
 * Network connectivity testcases.
 */
public class ConnectivityTest extends AndroidTestCase {

    private ConnectivityManager connectivity;

    //Connection attempt will be made to google.com
    private static final String  URL_NAME = "http://www.google.com";

    @Override
    protected void setUp() throws Exception {
        connectivity = (ConnectivityManager) getContext().
            getSystemService(Context.CONNECTIVITY_SERVICE);
        assertNotNull(connectivity);
    }

    /**
     * Test that there is an active network
     */
    public void testActiveConnectivity() {
        NetworkInfo networkInfo = connectivity.getActiveNetworkInfo();
        Log.d("ConnectivityTest", "validating active networks.");
        assertNotNull(networkInfo);
        assertEquals( NetworkInfo.State.CONNECTED, networkInfo.getState());
    }

    /**
     * Test that a connection can be made over the active network
     */
    public void testConnectionCreation() throws IOException {
        URL url = new URL(URL_NAME);
        Log.d("ConnectivityTest", "creating HTTP connection to google.com.");
        URLConnection connection = url.openConnection();
        connection.connect();
    }

}
