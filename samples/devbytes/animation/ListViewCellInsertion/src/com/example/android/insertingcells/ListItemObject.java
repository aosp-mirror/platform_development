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

package com.example.android.insertingcells;

/**
 * The data model for every cell in the ListView for this application. This model stores
 * a title, an image resource and a default cell height for every item in the ListView.
 */
public class ListItemObject {

    private String mTitle;
    private int mImgResource;
    private int mHeight;

    public ListItemObject(String title, int imgResource, int height) {
        super();
        mTitle = title;
        mImgResource = imgResource;
        mHeight = height;
    }

    public String getTitle() {
        return mTitle;
    }

    public int getImgResource() {
        return mImgResource;
    }

    public int getHeight() {
        return mHeight;
    }

}
