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

package com.example.android.actionbarcompat.shareactionprovider.content;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

/**
 * This class encapsulates a content item. Referencing the content's type, and the differing way
 * to reference the content (asset URI or resource id).
 */
public class ContentItem {
    // Used to signify an image content type
    public static final int CONTENT_TYPE_IMAGE = 0;
    // Used to signify a text/string content type
    public static final int CONTENT_TYPE_TEXT = 1;

    public final int contentType;
    public final int contentResourceId;
    public final String contentAssetFilePath;

    /**
     * Creates a ContentItem with the specified type, referencing a resource id.
     *
     * @param type - One of {@link #CONTENT_TYPE_IMAGE} or {@link #CONTENT_TYPE_TEXT}
     * @param resourceId - Resource ID to use for this item's content
     */
    public ContentItem(int type, int resourceId) {
        contentType = type;
        contentResourceId = resourceId;
        contentAssetFilePath = null;
    }

    /**
     * Creates a ContentItem with the specified type, referencing an asset file path.
     *
     * @param type - One of {@link #CONTENT_TYPE_IMAGE} or {@link #CONTENT_TYPE_TEXT}
     * @param assetFilePath - File path from the application's asset for this item's content
     */
    public ContentItem(int type, String assetFilePath) {
        contentType = type;
        contentAssetFilePath = assetFilePath;
        contentResourceId = 0;
    }

    /**
     * @return Uri to the content
     */
    public Uri getContentUri() {
        if (!TextUtils.isEmpty(contentAssetFilePath)) {
            // If this content has an asset, then return a AssetProvider Uri
            return Uri.parse("content://" + AssetProvider.CONTENT_URI + "/" + contentAssetFilePath);
        } else {
            return null;
        }
    }

    /**
     * Returns an {@link android.content.Intent} which can be used to share this item's content with other
     * applications.
     *
     * @param context - Context to be used for fetching resources if needed
     * @return Intent to be given to a ShareActionProvider.
     */
    public Intent getShareIntent(Context context) {
        Intent intent = new Intent(Intent.ACTION_SEND);

        switch (contentType) {
            case CONTENT_TYPE_IMAGE:
                intent.setType("image/jpg");
                // Bundle the asset content uri as the EXTRA_STREAM uri
                intent.putExtra(Intent.EXTRA_STREAM, getContentUri());
                break;

            case CONTENT_TYPE_TEXT:
                intent.setType("text/plain");
                // Get the string resource and bundle it as an intent extra
                intent.putExtra(Intent.EXTRA_TEXT, context.getString(contentResourceId));
                break;
        }

        return intent;
    }

}
