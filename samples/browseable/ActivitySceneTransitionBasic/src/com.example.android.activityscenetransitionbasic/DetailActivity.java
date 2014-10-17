/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.example.android.activityscenetransitionbasic;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.Volley;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.TextView;

/**
 * Our secondary Activity which is launched from {@link MainActivity}. Has a simple detail UI
 * which has a large banner image, title and body text.
 */
public class DetailActivity extends Activity {

    // Extra name for the ID parameter
    public static final String EXTRA_PARAM_ID = "detail:_id";

    // View name of the header image. Used for activity scene transitions
    public static final String VIEW_NAME_HEADER_IMAGE = "detail:header:image";

    // View name of the header title. Used for activity scene transitions
    public static final String VIEW_NAME_HEADER_TITLE = "detail:header:title";

    private NetworkImageView mHeaderImageView;
    private TextView mHeaderTitle;

    private ImageLoader mImageLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.details);

        // Construct an ImageLoader instance so that we can load images from the network
        mImageLoader = new ImageLoader(Volley.newRequestQueue(this), ImageMemoryCache.INSTANCE);

        // Retrieve the correct Item instance, using the ID provided in the Intent
        Item item = Item.getItem(getIntent().getIntExtra(EXTRA_PARAM_ID, 0));

        mHeaderImageView = (NetworkImageView) findViewById(R.id.imageview_header);
        mHeaderTitle = (TextView) findViewById(R.id.textview_title);

        // BEGIN_INCLUDE(detail_set_view_name)
        /**
         * Set the name of the view's which will be transition to, using the static values above.
         * This could be done in the layout XML, but exposing it via static variables allows easy
         * querying from other Activities
         */
        mHeaderImageView.setViewName(VIEW_NAME_HEADER_IMAGE);
        mHeaderTitle.setViewName(VIEW_NAME_HEADER_TITLE);
        // END_INCLUDE(detail_set_view_name)

        loadItem(item);
    }

    private void loadItem(Item item) {
        // Set the title TextView to the item's name and author
        mHeaderTitle.setText(getString(R.string.image_header, item.getName(), item.getAuthor()));

        final ImageMemoryCache cache = ImageMemoryCache.INSTANCE;
        Bitmap thumbnailImage = cache.getBitmapFromUrl(item.getThumbnailUrl());

        // Check to see if we already have the thumbnail sized image in the cache. If so, start
        // loading the full size image and display the thumbnail as a placeholder.
        if (thumbnailImage != null) {
            mHeaderImageView.setImageUrl(item.getPhotoUrl(), mImageLoader);
            mHeaderImageView.setImageBitmap(thumbnailImage);
            return;
        }

        // If we get here then we do not have either the full size or the thumbnail in the cache.
        // Here we just load the full size and make do.
        mHeaderImageView.setImageUrl(item.getPhotoUrl(), mImageLoader);
    }

}
