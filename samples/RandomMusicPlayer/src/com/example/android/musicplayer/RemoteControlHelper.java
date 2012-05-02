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

package com.example.android.musicplayer;

import android.app.PendingIntent;
import android.media.AudioManager;
import android.util.Log;

/**
 * Contains methods to handle registering/unregistering remote control clients.
 * These methods only run on ICS devices. On previous devices, all methods are
 * no-ops.
 */
public class RemoteControlHelper {
	private static final String TAG = "RemoteControlHelper";

	public static IRemoteControlClientCompat registerRemoteControlClient(
			AudioManager audioManager, PendingIntent intent) {

		IRemoteControlClientCompat remoteControlClient;
		try {
			remoteControlClient = new RemoteControlClientCompat(intent);
		} catch (NoClassDefFoundError ex) {
			Log.e(TAG, "Error creating remote control client", ex);

			remoteControlClient = new UnavailableControlClientCompat();
		}

		remoteControlClient.register(audioManager);

		return remoteControlClient;
	}

	public static void unregisterRemoteControlClient(AudioManager audioManager,
			IRemoteControlClientCompat remoteControlClient) {

		remoteControlClient.unregister(audioManager);
	}
}