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

import android.graphics.Bitmap;
import android.media.AudioManager;

public class UnavailableControlClientCompat implements IRemoteControlClientCompat {

	private class MetadataEditorCompat implements IMetadataEditorCompat {
		public IMetadataEditorCompat putString(int key, String value) {
			return this;
		}

		public IMetadataEditorCompat putBitmap(int key, Bitmap bitmap) {
			return this;
		}

		public IMetadataEditorCompat putLong(int key, long value) {
			return this;
		}

		public void clear() {
		}

		public void apply() {
		}
	}

	public IMetadataEditorCompat editMetadata(boolean startEmpty) {
		return new MetadataEditorCompat();
	}

	public void setPlaybackState(int state) {
	}

	public void setTransportControlFlags(int transportControlFlags) {
	}
	
	@Override
	public void register(AudioManager audioManager) {
	}
	
	@Override
	public void unregister(AudioManager audioManager) {
	}
}