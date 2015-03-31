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

package com.example.android.beamlargefiles;

import android.app.Activity;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;

/**
 * This class demonstrates how to use Beam to send files too large to transfer reliably via NFC.
 *
 * <p>While any type of data can be placed into a normal NDEF messages, NFC is not considered
 * "high-speed" communication channel. Large images can easily take > 30 seconds to transfer.
 * Because NFC requires devices to be in extremely close proximity, this is not ideal.
 *
 * <p>Instead, Android 4.2+ devices can use NFC to perform an initial handshake, before handing
 * off to a faster communication channel, such as Bluetooth, for file transfer.
 *
 * <p>The tradeoff is that this application will not be invoked on the receiving device. Instead,
 * the transfer will be handled by the OS. The user will be shown a notification when the transfer
 * is complete. Selecting the notification will open the file in the default viewer for its MIME-
 * type. (If it's important that your application be used to open the file, you'll need to register
 * an intent-filter to watch for the appropriate MIME-type.)
 */
public class BeamLargeFilesFragment extends Fragment implements NfcAdapter.CreateBeamUrisCallback {

    private static final String TAG = "BeamLargeFilesFragment";
    /** Filename that is to be sent for this activity. Relative to /assets. */
    private static final String FILENAME = "stargazer_droid.jpg";
    /** Content provider URI. */
    private static final String CONTENT_BASE_URI =
            "content://com.example.android.beamlargefiles.files/";

    /**
     * Standard lifecycle event. Registers a callback for large-file transfer, by calling
     * NfcAdapter.setBeamPushUrisCallback().
     *
     * Note: Like sending NDEF messages over standard Android Beam, there is also a non-callback
     * API available. See: NfcAdapter.setBeamPushUris().
     *
     * @param savedInstanceState Saved instance state.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        Activity a = getActivity();

        // Setup Beam to transfer a large file. Note the call to setBeamPushUrisCallback().
        // BEGIN_INCLUDE(setBeamPushUrisCallback)
        NfcAdapter nfc = NfcAdapter.getDefaultAdapter(a);
        if (nfc != null) {
            Log.w(TAG, "NFC available. Setting Beam Push URI callback");
            nfc.setBeamPushUrisCallback(this, a);
        } else {
            Log.w(TAG, "NFC is not available");
        }
        // END_INCLUDE(setBeamPushUrisCallback)
    }

    /**
     * Callback for Beam events (large file version). The return value here should be an array of
     * content:// or file:// URIs to send.
     *
     * Note that the system must have read access to whatever URIs are provided here.
     *
     * @param nfcEvent NFC event which triggered callback
     * @return URIs to be sent to remote device
     */
    // BEGIN_INCLUDE(createBeamUris)
    @Override
    public Uri[] createBeamUris(NfcEvent nfcEvent) {
        Log.i(TAG, "Beam event in progress; createBeamUris() called.");
        // Images are served using a content:// URI. See AssetProvider for implementation.
        Uri photoUri = Uri.parse(CONTENT_BASE_URI + FILENAME);
        Log.i(TAG, "Sending URI: " + photoUri);
        return new Uri[] {photoUri};
    }
    // END_INCLUDE(createBeamUris)
}
