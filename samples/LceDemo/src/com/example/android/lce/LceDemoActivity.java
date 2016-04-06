/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.example.android.lce;

import android.annotation.TargetApi;
import android.app.Activity;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

public class LceDemoActivity extends Activity {
    private ConnectivityManager connectityManager;

    @Override
    @TargetApi(23)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Default toolbar action handler.
        setContentView(R.layout.activity_lce_demo);
        final TextView text = (TextView) findViewById(R.id.lceText);
        text.setMovementMethod(new ScrollingMovementMethod());

        // Get an instance of ConnectivityManager.
        connectityManager = (ConnectivityManager)getSystemService(
            getApplicationContext().CONNECTIVITY_SERVICE);

        // Create a handler to update text on the screen.
        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                text.append((String)msg.obj);
                super.handleMessage(msg);
            }
        };

        // Now register the network call back function.
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        connectityManager.registerNetworkCallback(builder.build(),
            new ConnectivityManager.NetworkCallback() {
            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities cap) {
                Message message = handler.obtainMessage();
                message.obj = "New downlink capacity received: " +
                    cap.getLinkDownstreamBandwidthKbps() + " kbps on network: "
                    + network.toString() + "\n";
                handler.sendMessage(message);
            }
        });

        // Regsiter button action callback function, which pulls the LCE service
        // for bandwidth update.
        final Button button = (Button) findViewById(R.id.lceButton);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for(Network network : connectityManager.getAllNetworks()) {
                    // No-op for network interface not supporting LCE.
                    connectityManager.requestBandwidthUpdate(network);
                }
            }
        });
    }
}
