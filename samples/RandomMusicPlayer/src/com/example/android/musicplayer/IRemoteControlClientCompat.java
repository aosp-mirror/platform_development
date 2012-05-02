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

/**
 * RemoteControlClient enables exposing information meant to be consumed by remote controls capable
 * of displaying metadata, artwork and media transport control buttons. A remote control client
 * object is associated with a media button event receiver. This event receiver must have been
 * previously registered with
 * {@link android.media.AudioManager#registerMediaButtonEventReceiver(android.content.ComponentName)}
 * before the RemoteControlClient can be registered through
 * {@link android.media.AudioManager#registerRemoteControlClient(android.media.RemoteControlClient)}.
 */
public interface IRemoteControlClientCompat {

    /**
     * Class used to modify metadata in a {@link android.media.RemoteControlClient} object. Use
     * {@link android.media.RemoteControlClient#editMetadata(boolean)} to create an instance of an
     * editor, on which you set the metadata for the RemoteControlClient instance. Once all the
     * information has been set, use {@link #apply()} to make it the new metadata that should be
     * displayed for the associated client. Once the metadata has been "applied", you cannot reuse
     * this instance of the MetadataEditor.
     */
    public static interface IMetadataEditorCompat {
    	
    	public static final int METADATA_KEY_ARTWORK = 100;
    	
        /**
         * Adds textual information to be displayed.
         * Note that none of the information added after {@link #apply()} has been called,
         * will be displayed.
         * @param key The identifier of a the metadata field to set. Valid values are
         *      {@link android.media.MediaMetadataRetriever#METADATA_KEY_ALBUM},
         *      {@link android.media.MediaMetadataRetriever#METADATA_KEY_ALBUMARTIST},
         *      {@link android.media.MediaMetadataRetriever#METADATA_KEY_TITLE},
         *      {@link android.media.MediaMetadataRetriever#METADATA_KEY_ARTIST},
         *      {@link android.media.MediaMetadataRetriever#METADATA_KEY_AUTHOR},
         *      {@link android.media.MediaMetadataRetriever#METADATA_KEY_COMPILATION},
         *      {@link android.media.MediaMetadataRetriever#METADATA_KEY_COMPOSER},
         *      {@link android.media.MediaMetadataRetriever#METADATA_KEY_DATE},
         *      {@link android.media.MediaMetadataRetriever#METADATA_KEY_GENRE},
         *      {@link android.media.MediaMetadataRetriever#METADATA_KEY_TITLE},
         *      {@link android.media.MediaMetadataRetriever#METADATA_KEY_WRITER}.
         * @param value The text for the given key, or {@code null} to signify there is no valid
         *      information for the field.
         * @return Returns a reference to the same MetadataEditor object, so you can chain put
         *      calls together.
         */
        public IMetadataEditorCompat putString(int key, String value);

        /**
         * Sets the album / artwork picture to be displayed on the remote control.
         * @param key the identifier of the bitmap to set. The only valid value is
         *      {@link #METADATA_KEY_ARTWORK}
         * @param bitmap The bitmap for the artwork, or null if there isn't any.
         * @return Returns a reference to the same MetadataEditor object, so you can chain put
         *      calls together.
         * @throws IllegalArgumentException
         * @see android.graphics.Bitmap
         */
        public IMetadataEditorCompat putBitmap(int key, Bitmap bitmap);

        /**
         * Adds numerical information to be displayed.
         * Note that none of the information added after {@link #apply()} has been called,
         * will be displayed.
         * @param key the identifier of a the metadata field to set. Valid values are
         *      {@link android.media.MediaMetadataRetriever#METADATA_KEY_CD_TRACK_NUMBER},
         *      {@link android.media.MediaMetadataRetriever#METADATA_KEY_DISC_NUMBER},
         *      {@link android.media.MediaMetadataRetriever#METADATA_KEY_DURATION} (with a value
         *      expressed in milliseconds),
         *      {@link android.media.MediaMetadataRetriever#METADATA_KEY_YEAR}.
         * @param value The long value for the given key
         * @return Returns a reference to the same MetadataEditor object, so you can chain put
         *      calls together.
         * @throws IllegalArgumentException
         */
        public IMetadataEditorCompat putLong(int key, long value);

        /**
         * Clears all the metadata that has been set since the MetadataEditor instance was
         * created with {@link android.media.RemoteControlClient#editMetadata(boolean)}.
         */
        public void clear();

        /**
         * Associates all the metadata that has been set since the MetadataEditor instance was
         * created with {@link android.media.RemoteControlClient#editMetadata(boolean)}, or since
         * {@link #clear()} was called, with the RemoteControlClient. Once "applied", this
         * MetadataEditor cannot be reused to edit the RemoteControlClient's metadata.
         */
        public void apply();
    }

    /**
     * Creates a {@link android.media.RemoteControlClient.MetadataEditor}.
     * @param startEmpty Set to false if you want the MetadataEditor to contain the metadata that
     *     was previously applied to the RemoteControlClient, or true if it is to be created empty.
     * @return a new MetadataEditor instance.
     */
	public IMetadataEditorCompat editMetadata(boolean startEmpty);

    /**
     * Sets the current playback state.
     * @param state The current playback state, one of the following values:
     *       {@link android.media.RemoteControlClient#PLAYSTATE_STOPPED},
     *       {@link android.media.RemoteControlClient#PLAYSTATE_PAUSED},
     *       {@link android.media.RemoteControlClient#PLAYSTATE_PLAYING},
     *       {@link android.media.RemoteControlClient#PLAYSTATE_FAST_FORWARDING},
     *       {@link android.media.RemoteControlClient#PLAYSTATE_REWINDING},
     *       {@link android.media.RemoteControlClient#PLAYSTATE_SKIPPING_FORWARDS},
     *       {@link android.media.RemoteControlClient#PLAYSTATE_SKIPPING_BACKWARDS},
     *       {@link android.media.RemoteControlClient#PLAYSTATE_BUFFERING},
     *       {@link android.media.RemoteControlClient#PLAYSTATE_ERROR}.
     */
    public void setPlaybackState(int state);

    /**
     * Sets the flags for the media transport control buttons that this client supports.
     * @param transportControlFlags A combination of the following flags:
     *      {@link android.media.RemoteControlClient#FLAG_KEY_MEDIA_PREVIOUS},
     *      {@link android.media.RemoteControlClient#FLAG_KEY_MEDIA_REWIND},
     *      {@link android.media.RemoteControlClient#FLAG_KEY_MEDIA_PLAY},
     *      {@link android.media.RemoteControlClient#FLAG_KEY_MEDIA_PLAY_PAUSE},
     *      {@link android.media.RemoteControlClient#FLAG_KEY_MEDIA_PAUSE},
     *      {@link android.media.RemoteControlClient#FLAG_KEY_MEDIA_STOP},
     *      {@link android.media.RemoteControlClient#FLAG_KEY_MEDIA_FAST_FORWARD},
     *      {@link android.media.RemoteControlClient#FLAG_KEY_MEDIA_NEXT}
     */
    public void setTransportControlFlags(int transportControlFlags);

	public void register(AudioManager audioManager);

	public void unregister(AudioManager audioManager);
}