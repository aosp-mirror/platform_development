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
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.RemoteControlClient;
import android.media.RemoteControlClient.MetadataEditor;
import android.os.Looper;

public class RemoteControlClientCompat implements IRemoteControlClientCompat {

	private RemoteControlClient mActualRemoteControlClient;

	public RemoteControlClientCompat(PendingIntent pendingIntent) {
		mActualRemoteControlClient = new RemoteControlClient(pendingIntent);
	}

	public RemoteControlClientCompat(PendingIntent pendingIntent, Looper looper) {
		mActualRemoteControlClient = new RemoteControlClient(pendingIntent,
				looper);
	}

	private class MetadataEditorCompat implements IMetadataEditorCompat {

		private MetadataEditor mActualMetadataEditor;

		private MetadataEditorCompat(boolean startEmpty) {
			mActualMetadataEditor = mActualRemoteControlClient
					.editMetadata(startEmpty);
		}

		public MetadataEditorCompat putString(int key, String value) {
			mActualMetadataEditor.putString(key, value);

			return this;
		}

		public MetadataEditorCompat putBitmap(int key, Bitmap bitmap) {
			mActualMetadataEditor.putBitmap(key, bitmap);

			return this;
		}

		public MetadataEditorCompat putLong(int key, long value) {
			mActualMetadataEditor.putLong(key, value);
			return this;
		}

		public void clear() {
			mActualMetadataEditor.clear();
		}

		public void apply() {
			mActualMetadataEditor.apply();
		}
	}

	public IMetadataEditorCompat editMetadata(boolean startEmpty) {
		return new MetadataEditorCompat(startEmpty);
	}

	public void setPlaybackState(int state) {
		mActualRemoteControlClient.setPlaybackState(state);
	}

	public void setTransportControlFlags(int transportControlFlags) {
		mActualRemoteControlClient
				.setTransportControlFlags(transportControlFlags);
	}
	
	@Override
	public void register(AudioManager audioManager) {
		audioManager.registerRemoteControlClient(mActualRemoteControlClient);
	}
	
	@Override
	public void unregister(AudioManager audioManager) {
		audioManager.unregisterRemoteControlClient(mActualRemoteControlClient);
	}
}