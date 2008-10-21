/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.example.android.home;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.Gallery.LayoutParams;

import java.io.IOException;
import java.io.InputStream;

/**
 * Wallpaper picker for the Home application. User can choose from
 * a gallery of stock photos.
 */
public class Wallpaper extends Activity implements
        AdapterView.OnItemSelectedListener, AdapterView.OnItemClickListener {
    
    private static final String LOG_TAG = "Home";

    private static final Integer[] THUMB_IDS = {
            R.drawable.bg_android_icon,
            R.drawable.bg_sunrise_icon,
            R.drawable.bg_sunset_icon,
    };

    private static final Integer[] IMAGE_IDS = {
            R.drawable.bg_android,
            R.drawable.bg_sunrise,
            R.drawable.bg_sunset,
    };

    private Gallery mGallery;
    private boolean mIsWallpaperSet;
        
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.wallpaper);

        mGallery = (Gallery) findViewById(R.id.gallery);
        mGallery.setAdapter(new ImageAdapter(this));
        mGallery.setOnItemSelectedListener(this);
        mGallery.setOnItemClickListener(this);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        mIsWallpaperSet = false;
    }

    public void onItemSelected(AdapterView parent, View v, int position, long id) {
        getWindow().setBackgroundDrawableResource(IMAGE_IDS[position]);
    }
    
    public void onItemClick(AdapterView parent, View v, int position, long id) {
        selectWallpaper(position);
    }

    /*
     * When using touch if you tap an image it triggers both the onItemClick and
     * the onTouchEvent causing the wallpaper to be set twice. Synchronize this
     * method and ensure we only set the wallpaper once.
     */
    private synchronized void selectWallpaper(int position) {
        if (mIsWallpaperSet) {
            return;
        }
        mIsWallpaperSet = true;
        try {
            InputStream stream = getResources().openRawResource(IMAGE_IDS[position]);
            setWallpaper(stream);
            setResult(RESULT_OK);
            finish();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Failed to set wallpaper " + e);
        }
    }

    public void onNothingSelected(AdapterView parent) {
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        selectWallpaper(mGallery.getSelectedItemPosition());
        return true;
    }

    public class ImageAdapter extends BaseAdapter {

        private Context mContext;
        
        public ImageAdapter(Context c) {
            mContext = c;
        }

        public int getCount() {
            return THUMB_IDS.length;
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(final int position, View convertView, ViewGroup parent) {
            ImageView i = new ImageView(mContext);

            i.setImageResource(THUMB_IDS[position]);
            i.setAdjustViewBounds(true);
            i.setLayoutParams(new Gallery.LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            i.setBackgroundResource(android.R.drawable.picture_frame);
            return i;
        }

    }

}

    
