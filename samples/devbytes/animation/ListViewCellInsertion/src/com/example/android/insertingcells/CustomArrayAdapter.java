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

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;

/**
 * This custom array adapter is used to populate the ListView in this application.
 * This adapter also maintains a map of unique stable ids for each object in the data set.
 * Since this adapter has to support the addition of a new cell to the 1ist index, it also
 * provides a mechanism to add a stable ID for new data that was recently inserted.
 */
public class CustomArrayAdapter extends ArrayAdapter<ListItemObject> {

    HashMap<ListItemObject, Integer> mIdMap = new HashMap<ListItemObject, Integer>();
    List<ListItemObject> mData;
    Context mContext;
    int mLayoutViewResourceId;
    int mCounter;

    public CustomArrayAdapter(Context context, int layoutViewResourceId,
                              List <ListItemObject> data) {
        super(context, layoutViewResourceId, data);
        mData = data;
        mContext = context;
        mLayoutViewResourceId = layoutViewResourceId;
        updateStableIds();
    }

    public long getItemId(int position) {
        ListItemObject item = getItem(position);
        if (mIdMap.containsKey(item)) {
            return mIdMap.get(item);
        }
        return -1;
    }

    public void updateStableIds() {
        mIdMap.clear();
        mCounter = 0;
        for (int i = 0; i < mData.size(); ++i) {
            mIdMap.put(mData.get(i), mCounter++);
        }
    }

    public void addStableIdForDataAtPosition(int position) {
        mIdMap.put(mData.get(position), ++mCounter);
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ListItemObject obj = mData.get(position);

        if(convertView == null) {
            LayoutInflater inflater = ((Activity)mContext).getLayoutInflater();
            convertView = inflater.inflate(mLayoutViewResourceId, parent, false);
        }

        convertView.setLayoutParams(new ListView.LayoutParams(ListView.LayoutParams
                .MATCH_PARENT, obj.getHeight()));

        ImageView imgView = (ImageView)convertView.findViewById(R.id.image_view);
        TextView textView = (TextView)convertView.findViewById(R.id.text_view);

        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(),
                obj.getImgResource(), null);

        textView.setText(obj.getTitle());
        imgView.setImageBitmap(CustomArrayAdapter.getCroppedBitmap(bitmap));

        return convertView;
    }

    /**
     * Returns a circular cropped version of the bitmap passed in.
     */
    public static Bitmap getCroppedBitmap(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(),
                Config.ARGB_8888);

        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        Canvas canvas = new Canvas(output);

        final Paint paint = new Paint();
        paint.setAntiAlias(true);

        int halfWidth = bitmap.getWidth() / 2;
        int halfHeight = bitmap.getHeight() / 2;

        canvas.drawCircle(halfWidth, halfHeight, Math.max(halfWidth, halfHeight), paint);

        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));

        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }
}