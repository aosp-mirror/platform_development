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

package com.example.android.wearable.recipeassistant;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class RecipeListAdapter implements ListAdapter {
    private String TAG = "RecipeListAdapter";

    private class Item {
        String title;
        String name;
        String summary;
        Bitmap image;
    }

    private List<Item> mItems = new ArrayList<Item>();
    private Context mContext;
    private DataSetObserver mObserver;

    public RecipeListAdapter(Context context) {
        mContext = context;
        loadRecipeList();
    }

    private void loadRecipeList() {
        JSONObject jsonObject = AssetUtils.loadJSONAsset(mContext, Constants.RECIPE_LIST_FILE);
        if (jsonObject != null) {
            List<Item> items = parseJson(jsonObject);
            appendItemsToList(items);
        }
    }

    private List<Item> parseJson(JSONObject json) {
        List<Item> result = new ArrayList<Item>();
        try {
            JSONArray items = json.getJSONArray(Constants.RECIPE_FIELD_LIST);
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                Item parsed = new Item();
                parsed.name = item.getString(Constants.RECIPE_FIELD_NAME);
                parsed.title = item.getString(Constants.RECIPE_FIELD_TITLE);
                if (item.has(Constants.RECIPE_FIELD_IMAGE)) {
                    String imageFile = item.getString(Constants.RECIPE_FIELD_IMAGE);
                    parsed.image = AssetUtils.loadBitmapAsset(mContext, imageFile);
                }
                parsed.summary = item.getString(Constants.RECIPE_FIELD_SUMMARY);
                result.add(parsed);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse recipe list: " + e);
        }
        return result;
    }

    private void appendItemsToList(List<Item> items) {
        mItems.addAll(items);
        if (mObserver != null) {
            mObserver.onChanged();
        }
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public Object getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater inf = LayoutInflater.from(mContext);
            view = inf.inflate(R.layout.list_item, null);
        }
        Item item = (Item) getItem(position);
        TextView titleView = (TextView) view.findViewById(R.id.textTitle);
        TextView summaryView = (TextView) view.findViewById(R.id.textSummary);
        ImageView iv = (ImageView) view.findViewById(R.id.imageView);

        titleView.setText(item.title);
        summaryView.setText(item.summary);
        if (item.image != null) {
            iv.setImageBitmap(item.image);
        } else {
            iv.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_noimage));
        }
        return view;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return mItems.isEmpty();
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        mObserver = observer;
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        mObserver = null;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    public String getItemName(int position) {
        return mItems.get(position).name;
    }
}
