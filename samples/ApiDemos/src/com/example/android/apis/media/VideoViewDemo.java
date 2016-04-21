/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.example.android.apis.media;

import android.content.ClipData;
import android.media.MediaPlayer;
import android.view.DragEvent;
import android.view.View;
import com.example.android.apis.R;
import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.widget.MediaController;
import android.widget.VideoView;

public class VideoViewDemo extends Activity {

    private VideoView mVideoView;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.videoview);
        mVideoView = (VideoView) findViewById(R.id.surface_view);

        initPlayer(Uri.parse("android.resource://" + getPackageName() +
                "/" + R.raw.videoviewdemo));

        mVideoView.setOnDragListener(mDragListener);
    }

    private void initPlayer(Uri uri) {
        mVideoView.setVideoURI(uri);
        mVideoView.setMediaController(new MediaController(this));
        mVideoView.requestFocus();
    }

    private View.OnDragListener mDragListener = new View.OnDragListener() {
        @Override
        public boolean onDrag(View v, DragEvent event) {
            if (event.getAction() != DragEvent.ACTION_DROP) {
                return true;
            }
            ClipData clipData = event.getClipData();
            if (clipData.getItemCount() != 1) {
                return false;
            }
            ClipData.Item item = clipData.getItemAt(0);
            Uri uri = item.getUri();
            if (uri == null) {
                return false;
            }
            if (requestDragAndDropPermissions(event) == null) {
                return false;
            }
            initPlayer(uri);
            mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                public void onPrepared(MediaPlayer mediaPlayer) {
                    mVideoView.start();
                }
            });
            return true;
        }
    };
}
